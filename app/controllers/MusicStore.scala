package controllers

import scala.concurrent.Future
import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper
import play.api.libs.json.Json
import models.Cypher
import scala.util.Success
import scala.util.Failure
import org.slf4j.LoggerFactory

object MusicStore {
  val log = LoggerFactory.getLogger("MusicStore")
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  def getOrCreateSong(originTitle: String): Future[Song] = {
    Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} RETURN s.videoId")
      .on("ot" -> originTitle).getData
      .flatMap(data => {
        if (data.nonEmpty) {
          //use stored videoId and update the status
          val videoId = data.head.head
          val song = getSong(originTitle, videoId)
          song.onComplete {
            case Success(_) => {
              Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} SET s.status = 'OK'")
                .on("ot" -> originTitle).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
            case Failure(ex) => {
              log.error("YT error " + originTitle, ex)
              Cypher("MATCH (s:Song) WHERE s.originTitle = {ot} SET s.status = 'error'")
                .on("ot" -> originTitle).execute
                .foreach(json => println(originTitle + "\n" + json))
            }
          }
          song
        } else {
          //add new song
          val newSong = getSong(originTitle)
          newSong.onComplete {
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
          newSong
        }
      })
  }

  private def getSong(originTitle: String, videoId: String = null): Future[Song] = {
    for {
      videoId <- if (videoId == null) getVideoId(originTitle) else Future.successful(videoId)
      (title, duration) <- getDetails(videoId)
    } yield {
      Song(originTitle, videoId, title, duration)
    }
  }

  private def getVideoId(originTitle: String): Future[String] = {
    //TODO change to queryString()
    WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
      + helper.urlEncode(originTitle)
      + "&type=video&videoEmbeddable=true&videoSyndicated=true&fields=items%2Fid&key="
      + googleKey)
      .get
      .map(response => (response.json \\ "videoId").head.as[String])
  }

  private def getDetails(videoId: String): Future[(String, Int)] = {
    WS.url("https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails&id="
      + videoId
      + "&fields=items(contentDetails%2Csnippet)&key="
      + googleKey)
      .get
      .map { response =>
        val title = (response.json \\ "title").head.as[String]
        val mAndS = (response.json \\ "duration").head.as[String].replaceAll("[PTS]", "").split("M")
        val duration = mAndS.head.toInt * 60 + mAndS.last.toInt
        (title, duration)
      }
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
      MusicStore.getOrCreateSong(title)
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

