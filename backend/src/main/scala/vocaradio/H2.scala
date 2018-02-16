package vocaradio

import slick.jdbc.H2Profile.api._
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext

object H2 extends LazyLogging {
  val db = Database.forConfig("h2")

  case class Song(query: String, id: Option[String], active: Boolean)
  class Songs(tag: Tag) extends Table[Song](tag, "songs") {
    def query = column[String]("query", O.PrimaryKey)
    def id = column[Option[String]]("id")
    def active = column[Boolean]("active")
    def * = (query, id, active) <> (Song.tupled, Song.unapply)
  }
  val songs = TableQuery[Songs]

  // def addSong(add: AddSong)(implicit ex: ExecutionContext) = {
  //   logger.info("AddSong " + add)
  //   db.run(songs += Song(add.query, add.id, true))
  //     .failed
  //     .foreach(t => logger.error("AddSong", t))
  // }

  def init()(implicit ec: ExecutionContext) = {
    db.run(songs.schema.create)
      .failed
      .foreach(t => logger.error("H2 init", t))
  }
}
