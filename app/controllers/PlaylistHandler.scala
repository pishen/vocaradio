package controllers

import java.util.Date

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success

import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.pattern.pipe
import models.MusicStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import scalax.io.Resource

class PlaylistHandler extends Actor {
  private val titles = Resource.fromFile("titles")
  private val googleKey = Resource.fromFile("google-api-key").lines().head
  private val broadcaster = context.actorSelection("../broadcaster")
  private val chatLogger = context.actorSelection("../chatLogger")
  
  private val lowerBound = 15
  private var randomTitles = Random.shuffle(titles.lines().toSeq)
  private var buffer = {
    val initSongs = randomTitles.take(lowerBound + 1).map(SongHolder(_))
    randomTitles = randomTitles.drop(lowerBound + 1)
    initSongs
  }
  private var nextSong = getNextAndFill()

  def receive = {
    case PlaylistHandler.CurrentSong => {
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
    case PlaylistHandler.Content => {
      Future.sequence(buffer.map(_.htmlString)).map(_.mkString) pipeTo sender
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
    val fill = buffer.size < lowerBound
    if (fill) {
      if(randomTitles.isEmpty){
        val bufferTitles = buffer.map(_.originTitle)
        randomTitles = Random.shuffle(titles.lines().toSeq.filterNot(bufferTitles contains _))
      } 
      buffer = buffer :+ SongHolder(randomTitles.head)
      randomTitles = randomTitles.tail
      //notify to update playlist when ready
      buffer.last.htmlString.foreach(str => {
        val json = Json.obj("type" -> "updateList",
          "remove" -> "0",
          "append" -> str)
        broadcaster ! ToAll(Json.stringify(json))
      })
    }else{
      //notify to update playlist
      val json = Json.obj("type" -> "updateList",
          "remove" -> 0)
      broadcaster ! ToAll(Json.stringify(json))
    }
    nextSong
  }
  
  private def getTime() = new Date().getTime() / 1000
  
  case class SongHolder(originTitle: String) {
    lazy val songF = MusicStore.getSong(originTitle)
    lazy val htmlString = songF.map(song => {
      <span><img src={ song.thumb } title={ song.title }/></span>.toString
    }).recover { case _ => <span></span>.toString }
  }
}

object PlaylistHandler {
  case object CurrentSong
  case object Content
}