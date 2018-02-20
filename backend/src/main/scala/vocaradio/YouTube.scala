package vocaradio

import java.time.Duration

import HttpHelpers._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext
import Global._
import CirceHelpers._

object YouTube {
  case class SearchItemId(videoId: String)
  case class SearchItem(id: SearchItemId)
  case class SearchResult(items: Seq[SearchItem])

  case class Snippet(title: String)
  case class ContentDetails(duration: String) {
    val durationJ = Duration.parse(duration)
  }
  case class Video(
    id: String,
    snippet: Snippet,
    contentDetails: ContentDetails
  )
  case class VideoResult(items: Seq[Video])

  def search(q: String) = {
    Uri("https://www.googleapis.com/youtube/v3/search")
      .withQuery(
        "key" -> googleApiKey,
        "q" -> q.replaceAll("[-|_]", " "),
        "part" -> "snippet",
        "type" -> "video",
        "videoEmbeddable" -> "true"
      )
      .getJson()
      .flatMap(_.asF[SearchResult])
      .map(_.items.head.id.videoId)
  }

  def getVideo(id: String) = {
    Uri("https://www.googleapis.com/youtube/v3/videos")
      .withQuery(
        "key" -> googleApiKey,
        "id" -> id,
        "part" -> "snippet,contentDetails"
      )
      .getJson()
      .flatMap(_.asF[VideoResult])
      .map(_.items.head)
  }
}
