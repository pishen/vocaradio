package controllers

import akka.actor.Actor
import scala.util.Random
import scala.io.Source
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import models.Song

class Playlist extends Actor {
  private val titles = Source.fromFile("titles").getLines.toIndexedSeq
  private val googleKey = Source.fromFile("google-api-key").getLines.toSeq.head

  def receive = {
    case Playlist.Current => {
      sender ! randomPick
    }
  }

  private def randomPick = {
    val title = titles(Random.nextInt(titles.length))
    val futureId =
      WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
        + title
        + "&type=video&fields=items%2Fid&key="
        + googleKey)
        .get
        .map(response => (response.json \ "items" \ "id" \ "videoId").as[String])
    val id = Await.result(futureId, 5.seconds).asInstanceOf[String]
    val futureDuration =
      WS.url("https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id="
        + id
        + "&fields=items%2FcontentDetails&key="
        + googleKey)
        .get
        .map(response => (response.json \ "items" \ "contentDetails" \ "duration").as[String])
        .map(str => {
          val mAndS = str.replaceAll("[PTS]", "").split("M")
          mAndS.head.toInt * 60 + mAndS.last.toInt
        })
    val duration = Await.result(futureId, 5.seconds).asInstanceOf[Int]
    Song(id, duration)
  }
}

object Playlist {
  case object Current
}