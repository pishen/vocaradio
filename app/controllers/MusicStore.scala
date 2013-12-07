package controllers

import scala.concurrent.Future

import org.anormcypher.Cypher
import org.anormcypher.CypherRow

import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper

class MusicStore {
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  def updateSong(originTitle: String): Future[Boolean] = {
    //println("checking " + originTitle)
    val inStore = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} RETURN s.videoId")
      .on("originTitle" -> originTitle).apply.collect {
        case CypherRow(videoId: String) => videoId
      }

    //println("getting Song")
    if (inStore.isEmpty) {
      //add new song
      getSong(originTitle).recover {
        case e: Exception => Song.error(originTitle)
      }.map {
        song =>
          //println("svaing")
          Cypher("""
                CREATE (s:Song {
                  originTitle : {originTitle},
                  videoId : {videoId},
                  title : {title},
                  duration : {duration}
                })
                """)
            .on("originTitle" -> originTitle,
              "videoId" -> song.videoId,
              "title" -> song.title,
              "duration" -> song.duration)
            .execute
      }
    } else {
      //check id status and update if needed
      Future.successful(false)
    }
  }

  private def getSong(originTitle: String): Future[Song] = {
    for {
      videoId <- getVideoId(originTitle)
      (title, duration) <- getDetails(videoId)
    } yield {
      Song(originTitle, videoId, title, duration)
    }
  }

  private def getVideoId(originTitle: String): Future[String] = {
    WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
      + helper.urlEncode(originTitle)
      + "&type=video&videoEmbeddable=true&videoSyndicated=true&fields=items%2Fid&key="
      + googleKey)
      .get
      .map(response => (response.json \\ "videoId").head.as[String])
  }

  private def getDetails(id: String): Future[(String, Int)] = {
    WS.url("https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails&id="
      + id
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
    val musicStore = new MusicStore
    titles.foreach {
      title =>
        musicStore.updateSong(title)
          .map(res => (if (res) "success" else "fail") + ": " + title).foreach(println)
    }
    println("main done")
  }

}

