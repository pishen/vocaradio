package vocaradio

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.H2Profile.api._

object SongBase extends LazyLogging {
  case class Song(query: String, id: Option[String], active: Boolean)
  class Songs(tag: Tag) extends Table[Song](tag, "songs") {
    def query = column[String]("query", O.PrimaryKey)
    def id = column[Option[String]]("id")
    def active = column[Boolean]("active")
    def * = (query, id, active) <> (Song.tupled, Song.unapply)
  }
  val songs = TableQuery[Songs]

  val adminId = WebServer.adminId

  def createTable()(implicit db: Database, ec: ExecutionContext) = {
    db.run(songs.schema.create)
      .failed
      .foreach(t => logger.error("Failed on creating songs table", t))
  }

  def createSinkAndSource()(
      implicit materializer: ActorMaterializer,
      ec: ExecutionContext,
      db: Database
  ) = {
    MergeHub
      .source[IncomingMessage]
      .filter(_.userIdOpt.map(_ == adminId).getOrElse(false))
      .mapAsync(1) {
        case IncomingMessage(AddSong(query, idOpt), socketId, _) =>
          db.run(songs += Song(query, idOpt, true)).map { _ =>
            List(OutgoingMessage(SongAdded(query), Some(socketId)))
          }
        case _ =>
          Future.successful(List.empty[OutgoingMessage])
      }
      .mapConcat(_.toList)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
