package controllers

import akka.actor.Actor
import scala.util.Random
import scala.io.Source
import play.api.libs.ws.WS
import views.html.helper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import models.Song
import java.util.Date

class Playlist extends Actor {
  private val titles = Source.fromFile("titles").getLines.toIndexedSeq
  private val googleKey = Source.fromFile("google-api-key").getLines.toSeq.head

  private var playing: Song = randomPick()
  private var startTime: Long = getCurrentTimeInSecond()

  def receive = {
    case Playlist.Current => {
      val timePassed = getCurrentTimeInSecond() - startTime
      if (timePassed < playing.duration) {
        sender ! (playing.id, timePassed.toInt)
      } else {
        playing = randomPick()
        startTime = getCurrentTimeInSecond()
        sender ! (playing.id, 0)
      }
    }
  }

  private def randomPick(): Song = {
    val title = helper.urlEncode(titles(Random.nextInt(titles.length)))
    try {
      val futureId =
        WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
          + title
          + "&type=video&fields=items%2Fid&key="
          + googleKey)
          .get
          .map(response => (response.json \\ "videoId").head.as[String])
      val id = Await.result(futureId, 5.seconds).asInstanceOf[String]
      val futureDuration =
        WS.url("https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id="
          + id
          + "&fields=items%2FcontentDetails&key="
          + googleKey)
          .get
          .map(response => (response.json \\ "duration").head.as[String])
          .map(str => {
            val mAndS = str.replaceAll("[PTS]", "").split("M")
            mAndS.head.toInt * 60 + mAndS.last.toInt
          })
      val duration = Await.result(futureDuration, 5.seconds).asInstanceOf[Int]
      Song(id, duration)
    } catch {
      case ex: Exception => randomPick()
    }
  }

  private def getCurrentTimeInSecond() = new Date().getTime() / 1000
}

object Playlist {
  case object Current
}