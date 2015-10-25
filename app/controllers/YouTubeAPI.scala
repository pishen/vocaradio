package controllers

import play.api.libs.ws._
import play.api.libs.json._
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._

object YouTubeAPI {
  val googleApiKey = ConfigFactory.load().as[String]("google.api.key")

  def getSong(id: String)(implicit ws: WSClient) = {
    ws.url("https://www.googleapis.com/youtube/v3/videos")
      .withQueryString(
        "part" -> "contentDetails,snippet",
        "id" -> id,
        "key" -> googleApiKey
      )
      .get()
      .map(_.json)
      .map { json =>
        val item = (json \ "items").as[Seq[JsValue]].head //may throw exception

        val title = (item \ "snippet" \ "title").as[String]
        val thumbnail = (item \ "snippet" \ "thumbnails" \ "medium" \ "url").as[String]
        val seconds = Period.parse((item \ "contentDetails" \ "duration").as[String]).toStandardSeconds().getSeconds()
        Song(id, title, thumbnail, seconds)
      }
  }

  def searchSong(query: String)(implicit ws: WSClient) = {
    ws.url("https://www.googleapis.com/youtube/v3/search")
      .withQueryString(
        "part" -> "snippet",
        "q" -> query.replaceAll("-", " "),
        "type" -> "video",
        "videoEmbeddable" -> "true",
        "videoSyndicated" -> "true",
        "key" -> googleApiKey
      )
      .get()
      .map(_.json)
      .map { json =>
        val item = (json \ "items").as[Seq[JsValue]].head //may throw exception
        (item \ "id" \ "videoId").as[String]
      }
  }
}
