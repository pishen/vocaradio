package vocaradio

import java.time._
import java.util.NoSuchElementException

import akka.stream.Materializer
import akka.stream.scaladsl._
import cats.data.OptionT
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Error
import scala.concurrent.Future
import scala.util.Random
import slick.jdbc.H2Profile.api._
import Global._
import SongBase.Song
import YouTube.Video

object Player extends LazyLogging {
  case class Playing(video: Video, startTime: Instant) {
    def isEnd = startTime
      .plus(video.contentDetails.durationJ)
      .isBefore(Instant.now())
  }
  case class PlayerState(
      playing: Option[Playing],
      queue: Seq[Video],
      pool: Seq[String]
  ) {
    def shiftIfEnd() = if (playing.map(_.isEnd).getOrElse(true)) {
      fill(shift = true)
    } else {
      Future((this, List.empty[Song]))
    }

    def fill(shift: Boolean) = {
      val maxQueueSize = if (shift) 26 else 25
      val resF = for {
        filledPool <- if (pool.size >= 100) {
          Future(pool)
        } else {
          SongBase.getQueries().map { queries =>
            pool ++ Random.shuffle(queries.diff(pool))
          }
        }
        songs <- {
          Future.sequence {
            filledPool.take(maxQueueSize - queue.size).map(SongBase.getSong)
          }.map(_.flatten)
        }
        (filledQueue, songsToUpdate) <- {
          Future.sequence {
            songs.map { song =>
              OptionT.fromOption[Future](song.id)
                .flatMapF(id => YouTube.getVideo(id))
                .orElse {
                  OptionT(YouTube.search(song.query))
                    .flatMapF(id => YouTube.getVideo(id))
                }
                .value
                .map(song -> _)
            }
          }.map { (seq: Seq[(Song, Option[Video])]) =>
            val filledQueue = seq.flatMap(_._2)
            val songsToUpdate = seq.flatMap { case (song, videoOpt) =>
              if (song.id.isEmpty != videoOpt.isEmpty) {
                Some(song.copy(id = videoOpt.map(_.id)))
              } else None
            }
            (filledQueue, songsToUpdate)
          }
        }
      } yield {
        val newState = this.copy(
          if (shift) {
            filledQueue.headOption.map(video => Playing(video, Instant.now))
          } else {
            playing
          },
          if (shift) filledQueue.drop(1) else filledQueue,
          filledPool.drop(maxQueueSize - queue.size)
        )
        (newState, songsToUpdate)
      }
      resF.recover {
        case e: Exception =>
          logger.error("Error on shifting", e)
          (PlayerState(None, List.empty, List.empty), List.empty[Song])
      }
    }
  }

  def createSinkAndSource() = {
    MergeHub
      .source[IncomingMessage]
      .scanAsync(
        PlayerState(None, List.empty, List.empty) -> List.empty[OutgoingMessage]
      ) {
        case ((state, _), incoming) =>
          logger.info(incoming.toString)
          incoming.msg match {
            case Ready =>
              state.shiftIfEnd().map { case (newState, songsToUpdate) =>
                // UPDATE songs
                songsToUpdate.foreach(SongBase.updateSong(song))
                //TODO: send the queue info to client
                newState -> newState.playing.map { p =>
                  OutgoingMessage(Load(p.video.id), Some(incoming.socketId))
                }.toList
              }
            case Resume =>
              state.shiftIfEnd().map { case (newState, songsToUpdate) =>
                // UPDATE songs
                songsToUpdate.foreach(SongBase.updateSong(song))
                //TODO: send the queue info to client
                newState -> newState.playing.map { p =>
                  OutgoingMessage(
                    Play(
                      p.video.id,
                      Duration.between(
                        p.startTime,
                        Instant.now
                      ).getSeconds.toInt
                    ),
                    Some(incoming.socketId)
                  )
                }.toList
              }
            case Ended =>
              state.shiftIfEnd().map { case (newState, songsToUpdate) =>
                // UPDATE songs
                songsToUpdate.foreach(SongBase.updateSong(song))
                //TODO: send the queue info to client
                newState -> newState.playing.map { p =>
                  OutgoingMessage(Play(p.video.id, 0), Some(incoming.socketId))
                }.toList
              }
            case _ =>
              Future(state -> List.empty[OutgoingMessage])
          }
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
