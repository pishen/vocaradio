package vocaradio

import akka.http.scaladsl.model.Uri
import cats.data.EitherT
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import scala.concurrent.Future

object YouTube extends LazyLogging {
  case class EmptyItemsException() extends Exception

  case class SearchItemId(videoId: String)
  case class SearchItem(id: SearchItemId)
  case class SearchResult(items: Seq[SearchItem])

  case class VideoResult(items: Seq[Video])

  def search(q: String): Future[Option[String]] = {
    val fe = Uri("https://www.googleapis.com/youtube/v3/search")
      .withQuery(
        "key" -> googleApiKey,
        "q" -> q.replaceAll("[-|_]", " "),
        "part" -> "snippet",
        "type" -> "video",
        "videoEmbeddable" -> "true"
      )
      .get[SearchResult]
    EitherT(fe)
      .leftMap { decodeException =>
        logger.error("Failed YouTube search", decodeException)
      }
      .toOption
      .subflatMap(_.items.headOption.map(_.id.videoId))
      .value
  }

  def getVideo(id: String): Future[Option[Video]] = {
    val fe = Uri("https://www.googleapis.com/youtube/v3/videos")
      .withQuery(
        "key" -> googleApiKey,
        "id" -> id,
        "part" -> "snippet,contentDetails"
      )
      .get[VideoResult]
    EitherT(fe)
      .leftMap { decodeException =>
        logger.error("Failed YouTube getVideo", decodeException)
      }
      .toOption
      .subflatMap(_.items.headOption)
      .value
  }
}
