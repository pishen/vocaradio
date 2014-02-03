package controllers

import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import scala.Array.canBuildFrom
import scala.concurrent.Future
import scala.util.Random
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import models.GetNodes
import models.Labels
import models.Neo4j
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
import akka.util.Timeout

object Application extends Controller {
  implicit val timeout = Timeout(5000)
  //actors
  val broadcaster = Akka.system.actorOf(Props[Broadcaster], "broadcaster")
  val playlist = Akka.system.actorOf(Props[Playlist], "playlist")
  val chatLogger = Akka.system.actorOf(Props[ChatLogger], "chatLogger")
  val userHandler = Akka.system.actorOf(Props[UserHandler], "userHandler")
  val neo4j = Akka.system.actorOf(Props[Neo4j], "neo4j")

  val sdf = new SimpleDateFormat("MM/dd HH:mm:ss")
  val bgUrls = Seq("assets/images/nouveau-fond.jpg")

  val playlistSize = 25
  val colSize = 5
  val listHeight = (((playlistSize - 1) / colSize + 1) * 114).toString + "px"

  def index = Action {
    Ok(views.html.index(bgUrls(Random.nextInt(bgUrls.length)), listHeight))
      .withHeaders("X-Frame-Options" -> "DENY")
  }

  def sync = Action.async { request =>
    //Logger.info("sync: " + request.remoteAddress)
    (playlist ? CurrentSong).mapTo[String].map(str => Ok(str))
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
      (userHandler ? InspectToken(token)).mapTo[String]
    }

    userID.foreach(id => {
      val checkedID = if (id == "628930919") "DJ" else id
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

    Ok("got msg")
  }

  def chatHistory = Action.async {
    (chatLogger ? GetHistory).mapTo[Seq[String]].map(chatLogs => {
      Ok(Json.obj("history" -> chatLogs.mkString))
    })
  }

  def listContent = Action.async {
    (playlist ? ListContent).mapTo[String].map(str => Ok(str))
  }

  def order = Action(parse.json) { request =>
    val json = request.body
    val name = (json \ "name").as[String]
    require(name != "")
    val token = (json \ "token").as[String]
    val videoId = (json \ "videoId").as[String]

    (userHandler ? InspectToken(token)).mapTo[String]
      .foreach(userId => playlist ! Order(videoId, name, userId))

    Ok("got order")
  }

  def voca = Action {
    Ok(views.html.voca())
  }

  def vocaRequest = Action.async(parse.tolerantText) { request =>
    val json = Json.parse(request.body)
    val action = (json \ "action").as[String]

    if (action == "status-error") {
      (neo4j ? GetNodes(Labels.song, "status", "error")).mapTo[Seq[Long]]
        .map(ids => {
          
        })
    } else {
      Future.successful(Ok("Not Implemented"))
    }

  }

}