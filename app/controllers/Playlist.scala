package controllers

import java.util.Date

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success

import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import models.Song
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource
import views.html.helper

class Playlist extends Actor {
  private val titlesResource = Resource.fromFile("titles")
  private val replacesResource = Resource.fromFile("replaces")
  private val googleKey = Resource.fromFile("google-api-key").lines().head

  private def currentSecond() = new Date().getTime() / 1000
  private var titleSeq = Random.shuffle(titlesResource.lines().toSeq)
  private var futureSong = pick().map(pair => (pair._1, currentSecond, pair._2))

  def receive = {
    case AskSong => {
      futureSong.value match {
        case Some(Success(t)) =>
          if (currentSecond - t._2 >= t._1.duration - 2) {
            futureSong = pick().map(pair => (pair._1, currentSecond, pair._2))
          }
        case Some(Failure(e)) =>
          futureSong = pick().map(pair => (pair._1, currentSecond, pair._2))
        case _ => //do nothing
      }
      futureSong.map(t => (t._1.id, (currentSecond - t._2).toInt, t._3)) pipeTo sender
    }
    case Refill =>
      titleSeq = titleSeq ++ Random.shuffle(titlesResource.lines().toSeq.filterNot(titleSeq contains _))
  }

  private def pick(): Future[(Song, String)] = {
    val replaces = replacesResource.lines().map(_.split(">>>")).map(a => (a.head, a.last)).toMap
    val title = titleSeq.head
    titleSeq = titleSeq.tail
    if (titleSeq.length < 11) self ! Refill
    for {
      id <- replaces.get(title) match {
        case Some(id) => Future.successful(id)
        case None     => getFutureId(title)
      }
      duration <- getFutureDuration(id)
    } yield (Song(id, duration), title)
  }

  private def getFutureId(title: String): Future[String] = {
    val futureId = WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
      + helper.urlEncode(title)
      + "&type=video&fields=items%2Fid&key="
      + googleKey)
      .get
      .map(response => (response.json \\ "videoId").head.as[String])
    futureId.onFailure {
      case e: Exception => Resource.fromFile("problem-title").write(title + "\n")
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
      case e: Exception => Resource.fromFile("problem-id").write(id + "\n")
    }
    futureDuration
  }
}

case object AskSong
case object Refill