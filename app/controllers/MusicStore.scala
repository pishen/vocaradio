package controllers

import scala.concurrent.Future
import org.anormcypher.Cypher
import org.anormcypher.CypherRow
import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper
import play.api.libs.concurrent.Akka
import akka.actor.Actor

object MusicStore {
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  def updateSong(originTitle: String): Future[Song] = {
    val inDB = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} RETURN s.videoId")
      .on("originTitle" -> originTitle).apply.collect {
        case CypherRow(vid: String) => vid
      }

    if (inDB.nonEmpty) {
      //update song in DB by the saved id if it's not error
      val videoId = inDB.head
      val songF = getSong(originTitle, if (videoId != "error") videoId else null)
      songF.foreach(song => {
        val res = Cypher("""
          MATCH (s:Song)
          WHERE s.originTitle = {originTitle}
          SET s.videoId = {videoId}, s.title = {title}, s.duration = {duration}
          """).on("originTitle" -> originTitle,
          "videoId" -> song.videoId,
          "title" -> song.title,
          "duration" -> song.duration).execute
        //TODO log the error for Cypher
      })
      songF
    } else {
      //add new song
      val songF = getSong(originTitle)
      songF.foreach(song => {
        val res = Cypher("""
          CREATE (s:Song {
            originTitle : {originTitle},
            videoId : {videoId},
            title : {title},
            duration : {duration}
          })
          """).on("originTitle" -> originTitle,
          "videoId" -> song.videoId,
          "title" -> song.title,
          "duration" -> song.duration).execute
        //TODO log the error for Cypher
      })
      songF
    }
  }

  private def getSong(originTitle: String, videoId: String = null): Future[Song] = {
    val f = for {
      videoId <- if (videoId == null) getVideoId(originTitle) else Future.successful(videoId)
      (title, duration) <- getDetails(videoId)
    } yield {
      Song(originTitle, videoId, title, duration)
    }
    f.recover { case e: Exception => Song.error(originTitle) }
  }

  private def getVideoId(originTitle: String): Future[String] = {
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
    titles.foreach(title => {
      MusicStore.updateSong(title).foreach(song => println(title + ": " + song.videoId))
    })
    println("main done")
  }

}

