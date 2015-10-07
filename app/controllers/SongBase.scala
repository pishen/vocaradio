package controllers

import akka.actor.Actor
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global
import better.files._
import play.api.libs.json._
import SongBase._
import scala.concurrent.Future

class SongBase extends Actor {
  var songMap = File("songs").lines
    .map(Json.parse)
    .map{ json =>
      val key = (json \ "key").as[String]
      val songOpt = (json \ "song").asOpt[Song]
      key -> songOpt
    }
    .toMap
  
  def receive = {
    case GetAllKeys =>
      sender ! songMap.keys.toSeq
    case GetSong(key) =>
      songMap.get(key).flatten match {
        case Some(song) => sender ! song
        case None => Future.failed(new NoSuchElementException) pipeTo sender
      }
    case SetSong(key, songOpt) =>
      songMap += key -> songOpt
      val content = songMap.toSeq.map{ case (key, songOpt) =>
        Json.stringify(Json.obj("key" -> key, "song" -> songOpt))
      }
      File("songs").overwrite("").appendLines(content: _*)
  }
}

object SongBase {
  case object GetAllKeys
  case class GetSong(key: String)
  case class SetSong(key: String, songOpt: Option[Song])
}
