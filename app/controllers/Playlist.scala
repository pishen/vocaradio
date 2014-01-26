package controllers

import Application.songPicker
import Application.playlistSize
import Application.colSize
import akka.actor.Actor
import akka.actor.actorRef2Scala
import models.Song
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsString

class Playlist extends Actor {
  private var playing: Option[(SongWrapper, Long)] = None
  private var list = Seq.empty[SongWrapper]

  for (i <- 1 to playlistSize) songPicker ! Pick

  def receive = {
    case CurrentSong => {
      //update if needed
      playing match {
        case None => playNext()
        case Some((wrapper, startTime)) =>
          if (currentTime() - startTime >= wrapper.song.duration) playNext()
      }
      //check updated result
      playing match {
        case None => sender ! "empty-list"
        case Some((wrapper, startTime)) => {
          val jsonBase = Json.obj("id" -> wrapper.song.videoId,
            "position" -> (currentTime() - startTime))
          val json = if (wrapper.userName != null) {
            jsonBase ++ Json.obj("userName" -> wrapper.userName)
          } else {
            jsonBase
          }
          sender ! Json.stringify(json)
        }
      }
    }
    case ListContent => {
      val str = list.zipWithIndex.map { case (w, i) => w.html(i) }.mkString
      val json = Json.obj("content" -> str)
      sender ! Json.stringify(json)
    }
    case AddSong(s) => {
      val newSong = SongWrapper(s)
      list = list :+ newSong
      //broadcast update
      val str = newSong.html(list.indices.last)
      broadcast(Json.obj("type" -> "updatePlaylist", "append" -> str))
    }
    case RemoveSong(ot) => //TODO impl
    case Order(videoId, userName) => {
      list.find(_.song.videoId == videoId) match {
        case None => sender ! "song-not-exist"
        case Some(w) => {
          if (w.userName == null) {
            val (l, r) = list.span(_.userName != null)
            list = (l :+ SongWrapper(w.song, userName)) ++ (r.filter(_.song.videoId != videoId))
            //broadcast update
            val seq = list.zipWithIndex.map {
              case (w2, i) =>
                val base = Json.obj("id" -> w2.song.videoId, "to" -> getStyle(i))
                if (w2.song == w.song) {
                  val append = <span class="order">點播: <span>{ userName }</span></span>.toString
                  base + ("orderedBy" -> JsString(append))
                } else base
            }
            broadcast(Json.obj("type" -> "updatePlaylist", "convert" -> seq))
          }
        }
      }
    }
  }

  private def playNext() = {
    if (list.isEmpty) {
      playing = None
    } else {
      val oldList = list
      playing = Some(list.head, currentTime())
      list = list.tail
      songPicker ! Pick
      //broadcast update
      val seq = oldList.zipWithIndex.map {
        case (w, i) =>
          val base = Json.obj("id" -> w.song.videoId)
          if (i == 0) base + ("remove" -> JsBoolean(true))
          else base + ("to" -> JsString(getStyle(i - 1)))
      }
      broadcast(Json.obj("type" -> "updatePlaylist", "convert" -> seq))
    }
  }

  private def getStyle(index: Int) = {
    val xi = index % colSize
    val yi = index / colSize
    val x = (if (yi % 2 == 1) colSize - 1 - xi else xi) * 194
    val y = yi * 114
    "translate3d(" + x + "px," + y + "px,0)"
  }

  case class SongWrapper(song: Song, userName: String = null) {
    def html(index: Int) = {
      val transform = getStyle(index)
      val style = "transform:" + transform + ";-webkit-transform:" + transform
      <div id={ song.videoId } class="song" style={ style }>
        <div class="overlap">
          <p>{ song.title }</p>
          {
            if (userName == null) <button class="order">點播</button>
            else <span class="order">點播: <span>{ userName }</span></span>
          }
        </div>
        <img src={ song.thumbMedium }></img>
      </div>.toString
    }
  }
}

case object CurrentSong
case object ListContent
case class AddSong(song: Song)
case class RemoveSong(originTitle: String)
case class Order(videoId: String, userName: String)