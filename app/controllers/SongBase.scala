package controllers

import akka.actor.Actor
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global
import better.files._
import play.api.libs.json._
import SongBase._
import scala.concurrent.Future

class SongBase extends Actor {
  //Map[key, (Option[song], notFound)]
  var songMap = File("songs").lines
    .map(Json.parse)
    .map { json =>
      val key = (json \ "key").as[String]
      val songOpt = (json \ "song").asOpt[Song]
      val notFound = (json \ "notFound").asOpt[Boolean].getOrElse(false)
      key -> (songOpt -> notFound)
    }
    .toMap

  private def writeSongMap() = {
    val content = songMap.toSeq.map {
      case (key, (songOpt, notFound)) =>
        Json.stringify(Json.obj("key" -> key, "song" -> songOpt, "notFound" -> notFound))
    }
    File("songs").overwrite("").appendLines(content: _*)
  }

  def receive = {
    case GetAllKeys =>
      sender ! songMap.keys.toSeq
    case GetSong(key) =>
      songMap.get(key).map(_._1).flatten match {
        case Some(song) => sender ! song
        case None => Future.failed(new NoSuchElementException) pipeTo sender
      }
    case SetSong(key, song) =>
      songMap += key -> (Some(song) -> false)
      writeSongMap()
    case SongNotFound(key) =>
      songMap += key -> (songMap.get(key).flatMap(_._1) -> true)
      writeSongMap()
  }
}

object SongBase {
  case object GetAllKeys
  case class GetSong(key: String)
  case class SetSong(key: String, song: Song)
  case class SongNotFound(key: String)
}
