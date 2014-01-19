package controllers

import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import scala.Array.canBuildFrom
import scala.concurrent.duration.DurationInt
import scala.util.Random
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
import scala.concurrent.Future

object Application extends Controller {
  implicit val timeout = Timeout((5).seconds)
  val broadcaster = Akka.system.actorOf(Props[Broadcaster], "broadcaster")
  val playlistHandler = Akka.system.actorOf(Props[PlaylistHandler], "playlistHandler")
  val chatLogger = Akka.system.actorOf(Props[ChatLogger], "chatLogger")
  val userStore = Akka.system.actorOf(Props[UserHandler], "userStore")
  val sdf = new SimpleDateFormat("MM/dd HH:mm:ss")
  val bgUrls = Seq("assets/images/nouveau-fond.jpg")

  def index = Action {
    Ok(views.html.index(bgUrls(Random.nextInt(bgUrls.length))))
      .withHeaders("X-Frame-Options" -> "DENY")
  }

  def sync = Action.async {
    (playlistHandler ? PlaylistHandler.CurrentSong).mapTo[(String, Int, String)]
      .map(t => {
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
    val name = (json \ "name").as[String]
    require(name != "")
    val token = (json \ "token").as[String]
    val msg = (json \ "msg").as[String]

    val userID = if (token == "guest") {
      Future.successful("guest")
    } else {
      (userStore ? InspectToken(token)).mapTo[String]
    }

    userID.foreach(id => {
      val checkedID = if (id == "628930919") id + "(DJ)" else id
      val chatLog =
        <strong class="color" title={ checkedID }>{ name }</strong>
        <p title={ sdf.format(new Date()) }>{
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

      //log the message to chatroom
      broadcaster ! ToAll(Json.stringify(Json.obj("type" -> "chat", "content" -> chatLog)))
      //notification
      broadcaster ! ToAll(
        Json.stringify(
          Json.obj("type" -> "notify",
            "title" -> name,
            "body" -> msg,
            "tag" -> "chat")))
      //log the message on server
      chatLogger ! ChatLog(chatLog)
    })

    Ok("Got msg")
  }

  def chatHistory = Action.async {
    (chatLogger ? GetHistory).mapTo[Seq[String]].map(chatLogs => {
      Ok(Json.obj("history" -> chatLogs.mkString))
    })
  }

  def listContent = Action.async {
    (playlistHandler ? PlaylistHandler.Content).mapTo[String].map(str => Ok(Json.obj("content" -> str)))
  }

}