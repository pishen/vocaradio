package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Song(id: String, title: String, thumbnail: String, seconds: Int) {
  val html = {
    //TODO use CSS background instead of img
    <div class="overlap">
      <a class="yt-link" href={ s"http://www.youtube.com/watch?v=${id}" } target="_blank">
        { title }
      </a>
      <button class="order">request</button>
    </div>
    <img src={ thumbnail }></img>
  }.mkString
}

object Song {
  implicit val songFormat: Format[Song] = (
    (__ \ "id").format[String] and
    (__ \ "title").format[String] and
    (__ \ "thumbnail").format[String] and
    (__ \ "seconds").format[Int]
  )(Song.apply, unlift(Song.unapply))
}
