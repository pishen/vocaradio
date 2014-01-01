package controllers

import java.util.Date

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success

import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import scalax.io.Resource

class Playlist extends Actor {
  private val titles = Resource.fromFile("titles")
  private val googleKey = Resource.fromFile("google-api-key").lines().head
  private val broadcaster = context.actorSelection("../broadcaster")
  private val chatLogger = context.actorSelection("../chatLogger")
  private def getTime() = new Date().getTime() / 1000
  private val lowerBound = 20
  private var buffer = Random.shuffle(titles.lines().toSeq).map(SongHolder(_))
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
    case ListContent => {
      Future.sequence(buffer.take(20).map(_.songF.map(song => {
        <img src={ song.thumb } title={ song.title } width="100"/>.toString
      }).recover { case _ => <img src="error" width="0"/>.toString })) pipeTo sender
    }
  }

  private def getNextAndFill() = {
    val nextSong = buffer.head.songF.map(song => {
      //song changed notification
      /*val content = <p class="light">{ song.title }</p>.toString
      broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> content)))
      chatLogger ! ChatLog(content)*/
      (song, getTime)
    })
    buffer = buffer.tail
    if (buffer.length < lowerBound) {
      val bufferTitles = buffer.map(_.originTitle)
      val newTitles = Random.shuffle(titles.lines().toSeq.filterNot(bufferTitles.contains(_)))
      buffer = buffer ++ newTitles.map(SongHolder(_))
    }
    broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "updateList")))
    nextSong
  }

  private def removeAndFill(title: String) = {
    //TODO
  }

  case class SongHolder(originTitle: String) {
    lazy val songF = MusicStore.getSong(originTitle)
  }
}

case object CurrentSong
case object ListContent