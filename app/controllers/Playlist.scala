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
  private val googleKey = Resource.fromFile("google-api-key").lines().head
  private val broadcaster = context.actorSelection("../broadcaster")
  private val chatLogger = context.actorSelection("../chatLogger")

  private def getTime() = new Date().getTime() / 1000
  private var titleSeq = Random.shuffle(titlesResource.lines().toSeq).take(21)
  private var nextSong = getNextAndFill()

  def receive = {
    case AskSong => {
      nextSong.value match {
        case Some(Success((song, start))) =>
          if (getTime - start > song.duration - 2) {
            nextSong = getNextAndFill()
          }
        case Some(Failure(e)) => nextSong = getNextAndFill()
        case None             => //song not retrieved yet
      }
      nextSong.map {
        case (song, start) =>
          (song.videoId, (getTime - start).toInt, song.originTitle)
      } pipeTo sender
    }
  }

  private def getNextAndFill() = {
    val nextSong = MusicStore.updateAndGetSong(titleSeq.head).map(song => {
      //song changed notification
      /*val content = <p class="light">{ song.title }</p>.toString
      broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> content)))
      chatLogger ! ChatLog(content)*/
      (song, getTime)
    })
    titleSeq = titleSeq.tail
    if (titleSeq.length < 20) {
      titleSeq = titleSeq :+ Random.shuffle(titlesResource.lines()).find(!titleSeq.contains(_)).get
    }
    nextSong
  }

  private def removeAndFill(title: String) = {
    //TODO
  }
}

case object AskSong