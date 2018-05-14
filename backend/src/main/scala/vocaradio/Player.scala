package vocaradio

import java.time._
import java.util.NoSuchElementException

import akka.stream.Materializer
import akka.stream.scaladsl._
import cats.data._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Error
import scala.concurrent.Future
import scala.util.Random
import slick.jdbc.H2Profile.api._
import SongBase.Song

object Player extends LazyLogging {
  case class OnAir(video: Video, startTimeOpt: Option[Instant]) {
    def startIfNot = {
      this.copy(startTimeOpt = startTimeOpt.orElse(Some(Instant.now)))
    }

    def position = {
      startTimeOpt
        .map(Duration.between(_, Instant.now).getSeconds.toInt)
        .getOrElse(0)
    }

    def isEnd = {
      startTimeOpt
        .map(
          _.plus(Duration.parse(video.contentDetails.duration)).isBefore(
            // plus 1s to avoid some inaccurate durations
            Instant.now().plusSeconds(1)
          )
        )
        .getOrElse(false)
    }
  }

  case class State(
      onAirOpt: Option[OnAir],
      pickables: Seq[Pickable],
      pool: Seq[String]
  ) {
    def fill(): Future[State] = {
      def getFilledPool(): Future[Seq[String]] = {
        if (pool.size >= 100) {
          Future(pool)
        } else {
          SongBase.getQueries().map { queries =>
            pool ++ Random.shuffle(queries.diff(pool))
          }
        }
      }

      def getAndUpdateVideo(query: String) = {
        SongBase
          .getSong(query)
          .toDeepOption
          .flatMap { song =>
            val videoOpt = song.idOpt.toDeepOption
              .flatMap { id =>
                YouTube.getVideo(id).asDeepOption
              }
              .orElse {
                YouTube.search(query).asDeepOption.flatMap { id =>
                  YouTube.getVideo(id).asDeepOption
                }
              }
            // UPDATE
            videoOpt.map(_.id).value.map { idOpt =>
              if (idOpt != song.idOpt) {
                logger.info(s"Update $query from ${song.idOpt} to $idOpt")
                SongBase.updateSong(song.copy(idOpt = idOpt))
              }
            }
            videoOpt
          }
      }

      def getFilledPickables(filledPool: Seq[String]) = {
        filledPool
          .take(30 - pickables.size)
          .map { query =>
            getAndUpdateVideo(query)
              .map(video => Pickable(video, None))
              .value
              .value
          }
          .toList
          .sequence
          .map { eithers =>
            eithers.flatMap(_.left.toOption).foreach { error =>
              logger.error(error.toString)
            }
            pickables ++ eithers.flatMap(_.toOption).flatten
          }
      }

      val resF = for {
        filledPool <- getFilledPool()
        filledPickables <- getFilledPickables(filledPool)
      } yield {
        this.copy(
          onAirOpt.orElse {
            filledPickables.headOption.map { pickable =>
              OnAir(pickable.video, None)
            }
          },
          filledPickables.drop(onAirOpt.map(_ => 0).getOrElse(1)),
          filledPool.drop(30 - pickables.size)
        )
      }
      resF.recover {
        case t: Throwable =>
          logger.error("Error on filling", t)
          this
      }
    }

    def dropIfEnd() = this.copy(onAirOpt = onAirOpt.filterNot(_.isEnd))
    def drop() = this.copy(onAirOpt = None)
    def startIfNot() = this.copy(onAirOpt = onAirOpt.map(_.startIfNot))
  }

  def createSinkAndSource() = {
    MergeHub
      .source[Pipe.In]
      .scanAsync(
        State(None, Seq.empty, Seq.empty) -> Seq.empty[Pipe.Out]
      ) {
        case ((state, _), in) =>
          logger.info(in.toString)

          def isAdmin() = in.userIdOpt.map(_ == adminId).getOrElse(false)

          def buildUpdatePlaylist(newState: State) = {
            UpdatePlaylist(newState.pickables.map(_.cleanUserId()))
          }

          in.msg match {
            case Right(Ready) =>
              state.dropIfEnd().fill().map { newState =>
                val loadOpt = newState.onAirOpt.map { onAir =>
                  Pipe.Out(Load(onAir.video.id), Some(in.socketId))
                }
                val updatePlaylist = Pipe.Out(
                  buildUpdatePlaylist(newState),
                  if (newState.pickables == state.pickables) {
                    Some(in.socketId)
                  } else {
                    None
                  }
                )
                newState -> (loadOpt.toSeq :+ updatePlaylist)
              }
            case Right(Resume(id, position)) =>
              state.dropIfEnd().fill().map(_.startIfNot()).map { newState =>
                val playOpt = newState.onAirOpt
                  .filter { onAir =>
                    // only send Play in these conditions
                    onAir.video.id != id ||
                    position < (onAir.position - 30) ||
                    position > onAir.position
                  }
                  .map { onAir =>
                    Pipe.Out(
                      Play(onAir.video.id, onAir.position),
                      Some(in.socketId)
                    )
                  }
                val updatePlaylistOpt =
                  if (newState.pickables != state.pickables) {
                    Some(Pipe.Out(buildUpdatePlaylist(newState), None))
                  } else {
                    None
                  }
                newState -> (playOpt.toSeq ++ updatePlaylistOpt)
              }
            case Right(Ended) =>
              state.dropIfEnd().fill().map(_.startIfNot()).map { newState =>
                val playOpt = newState.onAirOpt.map { onAir =>
                  Pipe.Out(Play(onAir.video.id, 0), Some(in.socketId))
                }
                val updatePlaylistOpt =
                  if (newState.pickables != state.pickables) {
                    Some(Pipe.Out(buildUpdatePlaylist(newState), None))
                  } else {
                    None
                  }
                newState -> (playOpt.toSeq ++ updatePlaylistOpt)
              }
            case Right(Drop) if isAdmin() =>
              state.drop().fill().map { newState =>
                val loadOpt = newState.onAirOpt.map { onAir =>
                  Pipe.Out(Load(onAir.video.id), None)
                }
                val updatePlaylist = Pipe.Out(
                  buildUpdatePlaylist(newState), None
                )
                newState -> (loadOpt.toSeq :+ updatePlaylist)
              }
            case _ =>
              Future(state -> List.empty[Pipe.Out])
          }
      }
      .mapConcat(_._2.toList)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
