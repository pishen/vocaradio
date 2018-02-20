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
  case class Playing(video: YouTube.Video, startTime: Instant) {
    def isEnd = startTime
      .plus(video.contentDetails.durationJ)
      .isBefore(Instant.now())
  }
  case class PlayerState(
      playing: Option[Playing],
      queue: List[String]
  ) {
    def shift() = {
      val stateF = for {
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
          .recoverWith { case _: NoSuchElementException =>
            YouTube.search(query).flatMap(id => YouTube.getVideo(id))
          }
          .recover { case e: NoSuchElementException =>
            logger.info(s"Set $query to error")
            SongBase.updateSong(song.copy(error = true))
            throw e
          }
      } yield {
        if (song.error) {
          SongBase.updateSong(song.copy(error = false))
        }
        if (song.id.map(_ != video.id).getOrElse(true)) {
          SongBase.updateSong(song.copy(id = Some(video.id)))
        }
        this.copy(
          Some(Playing(video, Instant.now)),
          filledQueue.drop(1)
        )
      }
      stateF.recover {
        case e: Exception =>
          logger.error("Error on shifting", e)
          PlayerState(None, queue.drop(1))
      }
    }
  }

  def createSinkAndSource() = {
    MergeHub
      .source[IncomingMessage]
      .scan(
        PlayerState(None, List.empty) -> List.empty[OutgoingMessage]
      ) {
        case ((state, _), wrapped) =>
          wrapped.msg match {
            case Join =>
              logger.info(wrapped.toString)
              state -> List.empty[OutgoingMessage]
            case Leave =>
              logger.info(wrapped.toString)
              state -> List.empty[OutgoingMessage]
            case _ =>
              state -> List.empty[OutgoingMessage]
          }
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
