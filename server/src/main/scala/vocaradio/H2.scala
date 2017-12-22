package vocaradio

import slick.jdbc.H2Profile.api._

object H2 {
  val db = Database.forConfig("h2")

  case class Song(query: String, id: Option[String])
  class Songs(tag: Tag) extends Table[Song](tag, "songs") {
    def query = column[String]("query", O.PrimaryKey)
    def id = column[Option[String]]("id")
    def * = (query, id) <> (Song.tupled, Song.unapply)
  }
  val songs = TableQuery[Songs]

  db.run(songs.schema.create)
}
