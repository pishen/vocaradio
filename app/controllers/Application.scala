package controllers

import java.net.MalformedURLException
import java.net.URL
import scala.Array.canBuildFrom
import scala.concurrent.duration.DurationInt
import scala.util.Random
import scala.xml.Utility
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket
import models.Song
import java.net.URLDecoder

object Application extends Controller {
  implicit val timeout = Timeout((5).seconds)
  val broadcaster = Akka.system.actorOf(Props[Broadcaster], "broadcaster")
  val playlist = Akka.system.actorOf(Props[Playlist], "playlist")
  val chatLogger = Akka.system.actorOf(Props[ChatLogger], "chatLogger")
  val bgUrls = Seq("http://res.nimg.jp/img/watch_zero/walls/wall_ginza.jpg",
    "http://res.nimg.jp/img/watch_zero/walls/wall_night_cruise.jpg",
    "http://res.nimg.jp/img/watch_zero/walls/wall_cloud.jpg",
    "http://res.nimg.jp/img/watch_zero/walls/wall_night.jpg")

  def index = Action {
    Ok(views.html.index(bgUrls(Random.nextInt(bgUrls.length))))
  }

  def sync = Action.async {
    (playlist ? CurrentSong).mapTo[(String, Int, String)].map(t => {
      Ok(Json.obj("id" -> t._1, "start" -> t._2, "originTitle" -> t._3))
    })
  }

  def ws = WebSocket.async[String] { request =>
    (broadcaster ? Join).mapTo[Enumerator[String]].map { out =>
      val in = Iteratee.foreach[String](msg => {
        broadcaster ! BCClientCount
      }).map(_ => {
        broadcaster ! Quit
        broadcaster ! BCClientCount
      })
      (in, out)
    }
  }

  def chat = Action(parse.json) { request =>
    val json = request.body
    val user = (json \ "user").as[String]
    require(user != "")
    val msg = (json \ "msg").as[String]

    val chatLog =
      <strong class="color">{ user }</strong>
      <p>{
        //decorate the url
        msg.split(" ").map { s =>
          try {
            new URL(s)
            <a target="_blank" href={ s }>{ s }</a>
          } catch {
            case e: MalformedURLException=> " " + s + " "
          }
        }
      }</p>.mkString

    broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> chatLog)))
    broadcaster ! ToAll(
      Json.stringify(
        Json.obj("type" -> "notify",
          "title" -> user,
          "body" -> msg,
          "tag" -> "chat")))
    chatLogger ! ChatLog(chatLog)

    Ok("Got msg")
  }

  def chatHistory = Action.async {
    (chatLogger ? GetHistory).mapTo[Seq[String]].map(chatLogs => {
      Ok(Json.obj("history" -> chatLogs.mkString))
    })
  }

  def listContent = Action.async {
    (playlist ? ListContent).mapTo[Seq[String]].map(imgs => Ok(Json.obj("imgs" -> imgs)))
  }

}