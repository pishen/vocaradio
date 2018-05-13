package vocaradio

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.H2Profile.api._

object SongBase extends LazyLogging {
  case class Song(
      query: String,
      idOpt: Option[String],
      active: Boolean
  )
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
        case Pipe.In(Right(AddSong(query, idOpt)), socketId, _) =>
          db.run(songs.insertOrUpdate(Song(query, idOpt, true))).map { _ =>
            List(Pipe.Out(SongAdded(query), Some(socketId)))
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
