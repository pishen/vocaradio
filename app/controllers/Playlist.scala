package controllers

import akka.actor.Actor
import akka.pattern.pipe
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
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import scalax.io.Resource

class Playlist extends Actor {
  private val titles = Resource.fromFile("titles").lines().toIndexedSeq
  private val replaces = Resource.fromFile("replaces").lines().map(_.split(">>>")).map(a => (a.head, a.last)).toMap
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  private def currentSecond() = new Date().getTime() / 1000
  private var futureSong = randomPick().map(pair => (pair._1, currentSecond, pair._2))

  def receive = {
    case Playlist.AskSong => {
      futureSong.value match {
        case Some(Success(t)) =>
          if (currentSecond - t._2 >= t._1.duration - 2) {
            futureSong = randomPick().map(pair => (pair._1, currentSecond, pair._2))
          }
        case Some(Failure(e)) =>
          futureSong = randomPick().map(pair => (pair._1, currentSecond, pair._2))
        case _ => //do nothing
      }
      futureSong.map(t => (t._1.id, (currentSecond - t._2).toInt, t._3)) pipeTo sender
    }
  }

  private def randomPick(): Future[(Song, String)] = {
    val title = titles(Random.nextInt(titles.length))
    for {
      id <- replaces.get(title) match {
        case Some(id) => Future.successful(id)
        case None     => getFutureId(title)
      }
      duration <- getFutureDuration(id)
    } yield (Song(id, Seq(duration, 360).min), title)
  }

  private def getFutureId(title: String): Future[String] = {
    val futureId = WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
      + helper.urlEncode(title)
      + "&type=video&fields=items%2Fid&key="
      + googleKey)
      .get
      .map(response => (response.json \\ "videoId").head.as[String])
    futureId.onFailure {
      case e: Exception => Resource.fromFile("problem-title").write(title)
    }
    futureId
  }

  private def getFutureDuration(id: String): Future[Int] = {
    val futureDuration = WS.url("https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id="
      + id
      + "&fields=items%2FcontentDetails&key="
      + googleKey)
      .get
      .map(response => (response.json \\ "duration").head.as[String])
      .map(str => {
        val mAndS = str.replaceAll("[PTS]", "").split("M")
        mAndS.head.toInt * 60 + mAndS.last.toInt
      })
    futureDuration.onFailure {
      case e: Exception => Resource.fromFile("problem-id").write(id)
    }
    futureDuration
  }
}

object Playlist {
  case object AskSong
}