package controllers

import scala.concurrent.Future

import org.anormcypher.Cypher
import org.anormcypher.CypherRow

import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper

object MusicStore {
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  def getOrCreateSong(originTitle: String): Future[Song] = {
    val inDB = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} RETURN s.videoId")
      .on("originTitle" -> originTitle).apply.collect {
        case CypherRow(vid: String) => vid
      }
    //TODO log Cypher error?

    if (inDB.nonEmpty) {
      //use stored videoId and update the status
      val videoId = inDB.head
      val song = getSong(originTitle, videoId)
      //TODO merge two Cyphers
      song.onSuccess {
        case song: Song => {
          val res = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} SET s.status = 'OK'")
            .on("originTitle" -> originTitle).execute
          //TODO log Cypher error
        }
      }
      song.onFailure {
        case ex: Exception => {
          val res = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} SET s.status = 'error'")
            .on("originTitle" -> originTitle).execute
          //TODO log Cypher error
        }
      }
      song
    } else {
      //add new song
      val newSong = getSong(originTitle)
      newSong.onSuccess {
        case song: Song => {
          val res = Cypher("CREATE (s:Song {originTitle:{originTitle}, videoId:{videoId}, status:'OK'})")
            .on("originTitle" -> originTitle, "videoId" -> song.videoId).execute
          //TODO log Cypher error
        }
      }
      newSong.onFailure {
        case ex: Exception => {
          val res = Cypher("CREATE (s:Song {originTitle:{originTitle}, videoId:'error', status:'error'})")
            .on("originTitle" -> originTitle).execute
          //TODO log Cypher error
        }
      }
      newSong
    }
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
    /*titles.foreach(title => {
      MusicStore.updateAndGetSong(title).foreach(song => println(title + " updated as " + song.videoId))
    })*/
    //update by replaces
    Resource.fromFile("replaces").lines().toSeq
      .map(_.split(">>>")).foreach(s => {
        val originTitle = s.head
        val newId = s.last
        val res = Cypher("MATCH (s:Song) WHERE s.originTitle = {originTitle} SET s.videoId = {videoId}")
          .on("originTitle" -> originTitle, "videoId" -> newId).execute
        println("replace " + originTitle + " with status " + res)
      })

    println("main done")
  }

}

