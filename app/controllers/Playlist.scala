package controllers

import akka.actor.Actor
import akka.pattern.pipe
import scala.util.Random
import scala.io.Source
import play.api.libs.ws.WS
import views.html.helper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import models.Song
import java.util.Date
import scala.concurrent.Future
import scala.util.Success

class Playlist extends Actor {
  private val titles = Source.fromFile("titles").getLines.toIndexedSeq
  private val googleKey = Source.fromFile("google-api-key").getLines.toSeq.head
  
  private def currentSecond() = new Date().getTime() / 1000
  private var futureSong = randomPick().map(song => (song, currentSecond)) 

  def receive = {
    case Playlist.AskSong => {
      futureSong.value match {
        case Some(Success(p)) =>
          if(p._1.duration < currentSecond - p._2){
            futureSong = randomPick().map(song => (song, currentSecond))
          }
        case _ => //future song not fetched yet, do nothing
      }
      futureSong.map(p => (p._1.id, (currentSecond - p._2).toInt)) pipeTo sender
    }
  }

  private def randomPick(): Future[Song] = {
    val title = helper.urlEncode(titles(Random.nextInt(titles.length)))
    
    val futureSong = for {
      id <- WS.url("https://www.googleapis.com/youtube/v3/search?part=id&maxResults=1&q="
        + title
        + "&type=video&fields=items%2Fid&key="
        + googleKey)
        .get
        .map(response => (response.json \\ "videoId").head.as[String])
      duration <- WS.url("https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id="
        + id
        + "&fields=items%2FcontentDetails&key="
        + googleKey)
        .get
        .map(response => (response.json \\ "duration").head.as[String])
        .map(str => {
          val mAndS = str.replaceAll("[PTS]", "").split("M")
          mAndS.head.toInt * 60 + mAndS.last.toInt
        })
    } yield Song(id, duration)
    
    futureSong recoverWith {
      case ex: Exception => randomPick()
    }
  }
}

object Playlist {
  case object AskSong
}