package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.json.Json
import models.Song
import play.api.mvc.WebSocket
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Concurrent
import controllers.ClientCounter._

object Application extends Controller {

  implicit val timeout = Timeout(5.seconds)
  val playlistActor = Akka.system.actorOf(Props[Playlist], "playlistActor")
  val clientCounter = Akka.system.actorOf(Props[ClientCounter], "clientCounter")
  val (out, channel) = Concurrent.broadcast[String]

  def index = Action {
    Ok(views.html.index())
  }

  def sync = Action.async {
    (playlistActor ? Playlist.AskSong).mapTo[(String, Int, String)].map(t => {
      Ok(Json.obj("id" -> t._1, "start" -> t._2, "originTitle" -> t._3))
    })
  }
  
  def ws = WebSocket.using[String](request => {
    clientCounter ! AddClient
    
    val in = Iteratee.foreach[String](msg => {
      (clientCounter ? Count).mapTo[Int].foreach(i => channel.push(i.toString))
    }).map(_ => {
      clientCounter ! RemoveClient
      (clientCounter ? Count).mapTo[Int].foreach(i => channel.push(i.toString))
    })
    
    (in, out)
  })

}