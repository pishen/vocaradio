package controllers

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.matching.Regex

import org.slf4j.LoggerFactory

import models.Cypher
import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper

object MusicStore {
  val log = LoggerFactory.getLogger("MusicStore")
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  //TODO change the logging and println parts
  def getSong(originTitle: String): Future[Song] = {
    Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} RETURN s.videoId")
      .on("ot" -> originTitle).getData
      .flatMap(data => {
        if (data.nonEmpty) {
          /* use stored videoId first, 
           * if stored videoId has error,
           * try to search for new id and update status */
          val videoId = data.head.head
          val songF = getSongFromYT(originTitle, videoId).recoverWith {
            case ex: Exception => getSongFromYT(originTitle)
          }
          songF.onComplete {
            case Success(s) => {
              Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} SET s.videoId = {id}, s.status = 'OK'")
                .on("ot" -> originTitle, "id" -> s.videoId).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
            case Failure(ex) => {
              log.error("YT error " + originTitle, ex)
              Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} SET s.status = 'error'")
                .on("ot" -> originTitle).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
          }
          songF
        } else {
          //add new song
          val newSongF = getSongFromYT(originTitle)
          newSongF.onComplete {
            case Success(s) => {
              Cypher("CREATE (s:Song {originTitle:{ot}, videoId:{id}, status:'OK'})")
                .on("ot" -> originTitle, "id" -> s.videoId).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
            case Failure(ex) => {
              log.error("YT error " + originTitle, ex)
              Cypher("CREATE (s:Song {originTitle:{ot}, videoId:'error', status:'error'})")
                .on("ot" -> originTitle).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
          }
          newSongF
        }
      })
  }

  private def getSongFromYT(originTitle: String, videoId: String = null): Future[Song] = {
    for {
      videoId <- if (videoId == null) getVideoId(originTitle) else Future.successful(videoId)
      (title, duration, thumb) <- getDetails(videoId)
    } yield {
      Song(originTitle, videoId, title, duration, thumb)
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

  private def getDetails(videoId: String): Future[(String, Int, String)] = {
    WS.url("https://www.googleapis.com/youtube/v3/videos")
      .withQueryString(
        "part" -> "snippet,contentDetails",
        "id" -> videoId,
        "fields" -> "items(contentDetails,snippet)",
        "key" -> googleKey)
      .get
      .map(response => {
        val title = (response.json \\ "title").head.as[String]
        val rgx = new Regex("""PT(\d+M)?(\d+S)""", "m", "s")
        val ptms = (response.json \\ "duration").head.as[String]
        val duration = {
          val mt = rgx.findFirstMatchIn(ptms).get
          val m = mt.group("m")
          (if (m != null) m.init.toInt * 60 else 0) + mt.group("s").init.toInt
        }
        val thumb = (response.json \\ "thumbnails").head.\("default").\("url").as[String]
        (title, duration, thumb)
      })
  }

}

object BatchUpdater {

  def main(args: Array[String]): Unit = {
    val titles = Resource.fromFile("titles").lines().toSeq
    //delete 'not in titles' songs
    /*val inDB = Cypher("MATCH (s:Song) RETURN s.originTitle").apply.collect {
      case CypherRow(originTitle: String) => originTitle
    }
    inDB.filterNot(titles contains _).foreach(originTitle => {
      val res = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} DELETE s")
        .on("originTitle" -> originTitle).execute
      println("delete " + originTitle + " with status " + res)
    })*/
    //update by titles
    titles.foreach(title => {
      MusicStore.getSong(title)
    })
    //update by replaces
    /*Resource.fromFile("replaces").lines().toSeq
      .map(_.split(">>>")).foreach(s => {
        val originTitle = s.head
        val newId = s.last
        Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} SET s.videoId = {id}")
          .on("ot" -> originTitle, "id" -> newId).execute
          .foreach(json => {
            println(originTitle)
            println(json)
          })
      })*/

  }

}

