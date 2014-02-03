package models

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.matching.Regex

import akka.pattern.ask
import controllers.Application.neo4j
import controllers.Application.timeout
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource

object MusicStore {
  val googleKey = Resource.fromFile("google-api-key").lines().head

  def getSong(originTitle: String): Future[Song] = {
    (neo4j ? GetNodes(Labels.song, "originTitle", originTitle)).mapTo[Seq[Long]]
      .flatMap(nodes => {
        if (nodes.nonEmpty) {
          /* use stored videoId first, 
           * if stored videoId has error,
           * try to search for new id and update status */
          val nodeId = nodes.head
          (neo4j ? GetProperties(nodeId, Seq("videoId"))).mapTo[Map[String, String]]
            .flatMap(m => {
              val oldVideoId = m.get("videoId").get
              val song = getSongFromYT(originTitle, oldVideoId).recoverWith {
                case ex: Exception => getSongFromYT(originTitle)
              }
              song.onComplete {
                case Success(v) => {
                  neo4j ! SetProperties(nodeId, Seq("videoId" -> v.videoId, "status" -> "OK"))
                }
                case Failure(e) => {
                  Logger.error("YT error " + originTitle, e)
                  neo4j ! SetProperties(nodeId, Seq("status" -> "error"))
                }
              }
              song
            })
        } else {
          //add new song
          val newSong = getSongFromYT(originTitle)
          newSong.onComplete {
            case Success(v) => {
              val nodeContent = Seq("originTitle" -> originTitle, "videoId" -> v.videoId, "status" -> "OK")
              neo4j ! CreateNode(Labels.song, nodeContent)
            }
            case Failure(e) => {
              Logger.error("YT error " + originTitle, e)
              val nodeContent = Seq("originTitle" -> originTitle, "videoId" -> "error", "status" -> "error")
              neo4j ! CreateNode(Labels.song, nodeContent)
            }
          }
          newSong
        }
      })
  }

  private def getSongFromYT(originTitle: String, videoId: String = null): Future[Song] = {
    for {
      videoId <- if (videoId == null) getVideoId(originTitle) else Future.successful(videoId)
      details <- getDetails(videoId)
    } yield {
      val title = (details.json \\ "title").head.as[String]
      val rgx = new Regex("""PT(\d+M)?(\d+S)""", "m", "s")
      val ptms = (details.json \\ "duration").head.as[String]
      val duration = {
        val mt = rgx.findFirstMatchIn(ptms).get
        val m = mt.group("m")
        (if (m != null) m.init.toInt * 60 else 0) + mt.group("s").init.toInt
      }
      val thumbMedium = (details.json \\ "thumbnails").head.\("medium").\("url").as[String]
      Song(originTitle, videoId, title, duration, thumbMedium)
    }
  }

  private def getVideoId(originTitle: String): Future[String] = {
    WS.url("https://www.googleapis.com/youtube/v3/search")
      .withQueryString(
        "part" -> "id",
        "maxResults" -> "1",
        "q" -> originTitle,
        "type" -> "video",
        "videoEmbeddable" -> "true",
        "videoSyndicated" -> "true",
        "fields" -> "items/id",
        "key" -> googleKey)
      .get
      .map(response => (response.json \\ "videoId").head.as[String])
  }

  private def getDetails(videoId: String) = {
    WS.url("https://www.googleapis.com/youtube/v3/videos")
      .withQueryString(
        "part" -> "snippet,contentDetails",
        "id" -> videoId,
        "fields" -> "items(contentDetails,snippet)",
        "key" -> googleKey)
      .get
  }

}
