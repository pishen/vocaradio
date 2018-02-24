package vocaradio

import java.time._
import java.util.NoSuchElementException

import akka.stream.Materializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Error
import scala.concurrent.Future
import scala.util.Random
import slick.jdbc.H2Profile.api._
import Global._

object Player extends LazyLogging {
  case class SongNotFoundException(errorSong: SongBase.Song) extends Exception
  case class Playing(video: YouTube.Video, startTime: Instant) {
    def isEnd = startTime
      .plus(video.contentDetails.durationJ)
      .isBefore(Instant.now())
  }
  case class PlayerState(
      playing: Option[Playing],
      queue: List[String]
  ) {
    def shiftIfEnd() = if (playing.map(_.isEnd).getOrElse(true)) {
      val resF = for {
        filledQueue <- if (queue.size >= 100) {
          Future(queue)
        } else {
          SongBase.getQueries().map { queries =>
            queue ++ Random.shuffle(queries.diff(queue))
          }
        }
        query <- Future(filledQueue.head)
        song <- SongBase.getSong(query)
        video <- Future(song.id.get)
          .flatMap(id => YouTube.getVideo(id))
          .recoverWith { case _: YouTube.EmptyItemsException =>
            YouTube.search(query).flatMap(id => YouTube.getVideo(id))
          }
          .recover { case e: YouTube.EmptyItemsException =>
            throw SongNotFoundException(song.copy(error = true))
          }
      } yield {
        val newSong = song.copy(
          id = Some(video.id),
          error = false
        )
        val newState = this.copy(
          Some(Playing(video, Instant.now)),
          filledQueue.drop(1)
        )
        // side effect
        if (newSong != song) {
          logger.info("Update " + newSong)
          SongBase.updateSong(newSong)
        }
        // // //
        newState
      }
      resF.recover {
        case SongNotFoundException(errorSong) =>
          logger.error(s"Error on getting video of ${errorSong.query}")
          // side effect
          SongBase.updateSong(errorSong)
          // // //
          PlayerState(None, queue.drop(1))
        case e: Exception =>
          logger.error("Error on shifting", e)
          PlayerState(None, queue.drop(1))
      }
    } else {
      Future(this)
    }
  }

  def createSinkAndSource() = {
    MergeHub
      .source[IncomingMessage]
      .scanAsync(
        PlayerState(None, List.empty) -> List.empty[OutgoingMessage]
      ) {
        case ((state, _), incoming) =>
          logger.info(incoming.toString)
          incoming.msg match {
            case Ready =>
              state.shiftIfEnd().map { newState =>
                newState -> newState.playing.map { p =>
                  OutgoingMessage(Load(p.video.id), Some(incoming.socketId))
                }.toList
              }
            case Resume =>
              state.shiftIfEnd().map { newState =>
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
              state.shiftIfEnd().map { newState =>
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
