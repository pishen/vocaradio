package vocaradio

import akka.http.scaladsl.model.Uri
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._

object YouTube extends LazyLogging {
  case class EmptyItemsException() extends Exception

  case class SearchItemId(videoId: String)
  case class SearchItem(id: SearchItemId)
  case class SearchResult(items: Seq[SearchItem])

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
      .get[SearchResult]
      .asEitherT
      .map(_.items.headOption.map(_.id.videoId))
      .value
  }

  def getVideo(id: String) = {
    Uri("https://www.googleapis.com/youtube/v3/videos")
      .withQuery(
        "key" -> googleApiKey,
        "id" -> id,
        "part" -> "snippet,contentDetails"
      )
      .get[VideoResult]
      .asEitherT
      .map(_.items.headOption)
      .value
  }
}
