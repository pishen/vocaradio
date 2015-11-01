package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Song(id: String, title: String, thumbnail: String, seconds: Int)

object Song {
  implicit val songFormat: Format[Song] = (
    (__ \ "id").format[String] and
    (__ \ "title").format[String] and
    (__ \ "thumbnail").format[String] and
    (__ \ "seconds").format[Int]
  )(Song.apply, unlift(Song.unapply))
}
