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
  private val titles = Resource.fromFile("titles")
  private val googleKey = Resource.fromFile("google-api-key").lines().head
  private val broadcaster = context.actorSelection("../broadcaster")
  private val chatLogger = context.actorSelection("../chatLogger")
  private def getTime() = new Date().getTime() / 1000
  private val bufferSize = 5
  private var buffer = Random.shuffle(titles.lines().toSeq).take(bufferSize + 1)
    .map(ot => (ot, MusicStore.getSong(ot)))
  private var nextSong = getNextAndFill()

  def receive = {
    case CurrentSong => {
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
    case ListContent(oldSeq) => {
      def getImgs() = Future.sequence(buffer.map(_._2.map(song => {
        <img src={ song.thumb } title={ song.title } width="100"/>.toString
      }).recover { case _ => "" }))

      val res = for {
        ots <- Future.successful(buffer.map(_._1))
        imgs <- getImgs()
      } yield (ots, imgs)
      
      res pipeTo sender
    }
  }

  private def getNextAndFill() = {
    val nextSong = buffer.head._2.map(song => {
      //song changed notification
      /*val content = <p class="light">{ song.title }</p>.toString
      broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> content)))
      chatLogger ! ChatLog(content)*/
      (song, getTime)
    })
    buffer = buffer.tail
    if (buffer.length < bufferSize) {
      val bufferTitles = buffer.map(_._1)
      val otNew = Random.shuffle(titles.lines()).find(!bufferTitles.contains(_)).get
      buffer = buffer :+ (otNew, MusicStore.getSong(otNew))
    }
    broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "updateList")))
    nextSong
  }

  private def removeAndFill(title: String) = {
    //TODO
  }
}

case object CurrentSong
case class ListContent(oldSeq: Seq[String])