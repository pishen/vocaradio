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
    case GetNotFounds =>
      //Seq[(key, id)]
      val res = songMap.toSeq.filter(_._2._2).map {
        case (key, (songOpt, notFound)) => (key, songOpt.map(_.id).getOrElse("NA"))
      }
      sender ! res
    case GetSongById(id) =>
      //Seq[key]
      val res = songMap.toSeq.collect {
        case (key, (Some(song), _)) if song.id == id => key
      }
      sender ! res
    case MergeKeys(keys) =>
      songMap = songMap.filterKeys(keys.contains)
      songMap ++= (keys -- songMap.keySet).map(key => key -> (None -> false))
      writeSongMap()
  }
}

object SongBase {
  case object GetAllKeys
  case class GetSong(key: String)
  case class SetSong(key: String, song: Song)
  case class SongNotFound(key: String)
  //message from admin
  case object GetNotFounds
  case class GetSongById(id: String)
  case class MergeKeys(keys: Set[String])
}
