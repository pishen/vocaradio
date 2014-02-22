package controllers

import scala.util.Random
import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import models.MusicStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scalax.io.Resource
import models.MusicStore
import java.util.Date

class SongPicker extends Actor {
  private val titles = Resource.fromFile("titles")

  private var buffer = Seq.empty[SongWrapper]
  private var status = (titles.lines().size, new Date())

  case class SongWrapper(originTitle: String) {
    lazy val song = MusicStore.getSong(originTitle)
  }

  def receive = {
    case Pick(t) => {
      if (buffer.size < 25) refill()
      val picked = buffer.head
      buffer = buffer.tail
      picked.song.map(AddSong.apply) pipeTo sender
      picked.song.onFailure {
        case e: Exception => {
          if (t < 7) self ! Pick(t + 1)
        }
      }
    }
    case StoreStatus => sender ! status
  }

  private def refill() = {
    val oldTitles = buffer.map(_.originTitle)
    val titlesToFill = titles.lines().map(_.dropRight(4)).filterNot(oldTitles.contains)
    val newWrappers = Random.shuffle(titlesToFill).map(SongWrapper.apply)
    buffer = buffer ++ newWrappers
    status = (titles.lines().size, new Date())
  }

}

case class Pick(times: Int)
case object StoreStatus
