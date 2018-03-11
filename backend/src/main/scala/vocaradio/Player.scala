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
import YouTube.RichContentDetails

object Player extends LazyLogging {
  case class Playing(video: Video, startTime: Instant) {
    def at = Duration.between(
      startTime,
      Instant.now
    ).getSeconds.toInt

    def isEnd = startTime
      .plus(video.contentDetails.durationJ)
      // plus 1s to avoid some inaccurate durations
      .isBefore(Instant.now().plusSeconds(1))
  }
  case class InternalPicker(name: String, id: String) {
    def toPicker = Picker(name)
  }
  case class PlayerState(
      playing: Option[Playing] = None,
      queue: Seq[(Video, Option[InternalPicker])] = Seq.empty,
      pool: Seq[String] = Seq.empty
  ) {
    def shiftIfEnd() = if (playing.map(_.isEnd).getOrElse(true)) {
      fill(shift = true)
    } else {
      Future((this, Seq.empty))
    }

    // return Future[(newState, songsToUpdate)]
    def fill(
      shift: Boolean
    ): Future[(PlayerState, Seq[Song])] = {
      val maxQueueSize = if (shift) 25 else 24
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
            val filledQueue = queue ++ seq.flatMap(_._2).map(_ -> None)
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
            filledQueue.headOption.map { case (video, pickerOpt) =>
              Playing(video, Instant.now)
            }
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
          (PlayerState(), Seq.empty)
      }
    }

    def getUpdatePlaylist() = {
      UpdatePlaylist(
        queue.map { case (video, internalPickerOpt) =>
          video -> internalPickerOpt.map(_.toPicker)
        }
      )
    }
  }

  def createSinkAndSource() = {
    MergeHub
      .source[Incoming]
      .scanAsync(
        PlayerState() -> Iterable.empty[Outgoing]
      ) {
        case ((state, _), incoming) =>
          logger.info(incoming.toString)
          incoming.msg match {
            case Ready =>
              state.shiftIfEnd().map {
                case (newState, songsToUpdate) =>
                  songsToUpdate.foreach(SongBase.updateSong)
                  val outgoings = newState.playing.map { p =>
                    Outgoing(Load(p.video.id), Some(incoming.socketId))
                  }.toSeq :+ Outgoing(
                    newState.getUpdatePlaylist(),
                    if (newState.queue == state.queue) {
                      Some(incoming.socketId)
                    } else None
                  )
                  newState -> outgoings
              }
            case Resume(id, at) =>
              state.shiftIfEnd().map {
                case (newState, songsToUpdate) =>
                  songsToUpdate.foreach(SongBase.updateSong)
                  val outgoings = newState.playing
                    .filter { p =>
                      p.video.id != id || at < (p.at - 30) || at > p.at
                    }
                    .map { p =>
                      Outgoing(
                        Play(p.video.id, p.at),
                        Some(incoming.socketId)
                      )
                    }
                    .++ {
                      if (newState.queue != state.queue) {
                        Some(Outgoing(newState.getUpdatePlaylist(), None))
                      } else None
                    }
                  newState -> outgoings
              }
            case Ended =>
              state.shiftIfEnd().map {
                case (newState, songsToUpdate) =>
                  songsToUpdate.foreach(SongBase.updateSong)
                  val outgoings = newState.playing.map { p =>
                    Outgoing(Play(p.video.id, 0), Some(incoming.socketId))
                  } ++ {
                    if (newState.queue != state.queue) {
                      Some(Outgoing(newState.getUpdatePlaylist(), None))
                    } else None
                  }
                  newState -> outgoings
              }
            case _ =>
              Future(state -> List.empty[Outgoing])
          }
      }
      .mapConcat(_._2.toList)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
