package controllers

import scala.util.Random

import Application.playlist
import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import models.MusicStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scalax.io.Resource

class SongPicker extends Actor {
  private val titles = Resource.fromFile("titles")

  private var buffer = Seq.empty[SongWrapper]

  case class SongWrapper(originTitle: String) {
    lazy val song = MusicStore.getSong(originTitle)
  }

  def receive = {
    case Pick(t) => {
      if (buffer.size < 25) refill()
      val picked = buffer.head
      buffer = buffer.tail
      picked.song.map(AddSong.apply) pipeTo playlist
      picked.song.onFailure {
        case e: Exception => {
          if(t < 20) self ! Pick(t + 1)
        }
      }
    }
  }

  private def refill() = {
    val oldTitles = buffer.map(_.originTitle)
    val newWrappers =
      Random.shuffle(titles.lines().filterNot(oldTitles.contains))
        .map(SongWrapper.apply)
    buffer = buffer ++ newWrappers
  }

}

case class Pick(times: Int)
