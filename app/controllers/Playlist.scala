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
import play.api.libs.json.Json

class Playlist extends Actor {
  private val titlesResource = Resource.fromFile("titles")
  private val replacesResource = Resource.fromFile("replaces")
  private val googleKey = Resource.fromFile("google-api-key").lines().head
  private val broadcaster = context.actorSelection("../broadcaster")
  private val chatLogger = context.actorSelection("../chatLogger")

  private def currentSecond() = new Date().getTime() / 1000
  private var titleSeq = Random.shuffle(titlesResource.lines().toSeq)
  private var futureSong = pickWithTime()

  def receive = {
    case AskSong => {
      futureSong.value match {
        case Some(Success(t)) =>
          if (currentSecond - t._2 >= t._1.duration - 2) {
            futureSong = pickWithTime()
          }
        case Some(Failure(e)) =>
          futureSong = pickWithTime()
        case _ => //do nothing
      }
      futureSong.map {
        case (song, startTime) =>
          (song.videoId, (currentSecond - startTime).toInt, song.originTitle)
      } pipeTo sender
    }
    case Refill =>
      titleSeq = titleSeq ++ Random.shuffle(titlesResource.lines().toSeq.filterNot(titleSeq contains _))
  }

  private def pickWithTime() = {
    pick().map {
      case song =>
        //val content = <p class="light">{ song.title }</p>.toString
        //broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> content)))
        //chatLogger ! ChatLog(content)
        (song, currentSecond)
    }
  }

  private def pick(): Future[Song] = {
    val replaces = replacesResource.lines().map(_.split(">>>")).map(a => (a.head, a.last)).toMap
    val originTitle = titleSeq.head
    titleSeq = titleSeq.tail
    if (titleSeq.length < 11) self ! Refill
    for {
      id <- replaces.get(originTitle) match {
        case Some(id) => Future.successful(id)
        case None     => getFutureId(originTitle)
      }
      (title, duration) <- getFutureDetails(id)
    } yield Song(originTitle, id, title, duration)
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

  private def getFutureDetails(id: String): Future[(String, Int)] = {
    val futureDetails = WS.url("https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails&id="
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
    futureDetails.onFailure {
      case e: Exception => Resource.fromFile("problem-id").write(id + "\n")
    }
    futureDetails
  }
}

case object AskSong
case object Refill