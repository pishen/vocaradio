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

object Application extends Controller {

  implicit val timeout = Timeout(5.seconds)
  val playlistActor = Akka.system.actorOf(Props[Playlist], "playlistActor")
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def sync = Action.async {
    (playlistActor ? Playlist.AskSong).mapTo[(String, Int)].map(p => {
      Ok(Json.obj("id" -> p._1, "start" -> p._2))
    })
  }
  
}