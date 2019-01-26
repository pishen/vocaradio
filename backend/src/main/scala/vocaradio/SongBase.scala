package vocaradio

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.H2Profile.api._
import Pipe.ClientMessage

object SongBase extends LazyLogging {
  class Songs(tag: Tag) extends Table[Song](tag, "songs") {
    def query = column[String]("query", O.PrimaryKey)
    def idOpt = column[Option[String]]("id")
    def active = column[Boolean]("active")
    def * = (query, idOpt, active) <> (Song.tupled, Song.unapply)
  }
  val songs = TableQuery[Songs]

  def createSinkAndSource() = {
    MergeHub
      .source[Pipe.In]
      .filter(_.userIdOpt.map(_ == adminId).getOrElse(false))
      .mapAsync(1) {
        case Pipe.In(ClientMessage(Save(song)), socketId, _) =>
          db.run(songs.insertOrUpdate(song)).map { _ =>
            List(Pipe.Out(Saved(song.query), Some(socketId)))
          }
        case Pipe.In(ClientMessage(BatchSave(song)), socketId, _) =>
          db.run(songs.insertOrUpdate(song)).map { _ =>
            List(Pipe.Out(BatchSaved(song.query), Some(socketId)))
          }
        case Pipe.In(ClientMessage(Delete(query)), socketId, _) =>
          db.run(songs.filter(_.query === query).delete).map { deleted =>
            List.fill(deleted)(Pipe.Out(Deleted(query), Some(socketId)))
          }
        case Pipe.In(ClientMessage(Id(id)), socketId, _) =>
          db.run(songs.filter(_.idOpt === id).result).map { songs =>
            List(Pipe.Out(ShowSongs(songs), Some(socketId)))
          }
        case _ =>
          Future.successful(List.empty[Pipe.Out])
      }
      .mapConcat(_.toList)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }

  def getQueries() = db.run {
    songs.filter(_.active).map(_.query).result
  }

  def getSong(query: String) = db.run {
    songs.filter(_.query === query).result.headOption
  }

  def updateSong(song: Song) = db.run {
    songs.filter(_.query === song.query).update(song)
  }
}
