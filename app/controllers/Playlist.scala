package controllers

import Application.clientHandler
import Application.songPicker
import akka.actor.Actor
import akka.actor.actorRef2Scala
import models.Song
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper

class Playlist extends Actor {
  private var playing: Option[(SongWrapper, Long)] = None
  private var list = Seq.empty[SongWrapper]

  val playlistSize = 25
  val colSize = 5
  
  def currentTime() = System.currentTimeMillis() / 1000
  
  for (i <- 1 to playlistSize) songPicker ! Pick(1)

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
      val newSong = SongWrapper(s, null, null)
      list = list :+ newSong
      //broadcast update
      val str = newSong.html(list.indices.last)
      clientHandler ! BroadcastJson(Json.obj("type" -> "updatePlaylist", "append" -> str))
    }
    case RemoveSong(ot) => //TODO impl
    case Order(videoId, userName, userId) => {
      list.find(_.song.videoId == videoId) match {
        case None => sender ! "song-not-exist"
        case Some(w) => {
          if (w.userId == null) {
            val (l, r) = list.splitAt(list.lastIndexWhere(_.userId == userId) + 1)
            val (rl, rr) = r.zipWithIndex.span {
              case (w2, i) =>
                val id = w2.userId
                val prefix = r.take(i).map(_.userId)
                id != null && !prefix.contains(id)
            }
            list = l ++ rl.map(_._1) ++
              Seq(SongWrapper(w.song, userName, userId)) ++
              rr.map(_._1).filter(_.song.originTitle != w.song.originTitle)

            //broadcast update
            val seq = list.zipWithIndex.map {
              case (w2, i) =>
                val base = Json.obj("id" -> w2.song.videoId, "to" -> getStyle(i))
                if (w2.song == w.song) {
                  val append = <span class="order">點播: <span>{ userName }</span></span>.toString
                  base + ("orderedBy" -> JsString(append))
                } else base
            }
            clientHandler ! BroadcastJson(Json.obj("type" -> "updatePlaylist", "convert" -> seq))
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
      songPicker ! Pick(1)
      //broadcast update
      val seq = oldList.zipWithIndex.map {
        case (w, i) =>
          val base = Json.obj("id" -> w.song.videoId)
          if (i == 0) base + ("remove" -> JsBoolean(true))
          else base + ("to" -> JsString(getStyle(i - 1)))
      }
      clientHandler ! BroadcastJson(Json.obj("type" -> "updatePlaylist", "convert" -> seq))
    }
  }

  private def getStyle(index: Int) = {
    val xi = index % colSize
    val yi = index / colSize
    val x = (if (yi % 2 == 1) colSize - 1 - xi else xi) * 194 + 10
    val y = yi * 114
    "translate3d(" + x + "px," + y + "px,0)"
  }

  case class SongWrapper(song: Song, userName: String, userId: String) {
    def html(index: Int) = {
      val transform = getStyle(index)
      val style = "transform:" + transform + ";-webkit-transform:" + transform
      <div id={ song.videoId } class="song" style={ style }>
        <div class="overlap">
          <a class="yt-link" href={ "http://www.youtube.com/watch?v=" + song.videoId } target="_blank">
            { song.title }
          </a>
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
case class Order(videoId: String, userName: String, userId: String)