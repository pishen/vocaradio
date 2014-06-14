package controllers

import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

import scala.Array.canBuildFrom
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import models.GetNodes
import models.GetNodesResponse
import models.GetProperties
import models.GetPropertiesResponse
import models.Labels
import models.Neo4j
import models.SetProperties
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket

object Application extends Controller {
  implicit val timeout = Timeout(5.seconds)
  //actors
  //val broadcaster = Akka.system.actorOf(Props[Broadcaster])
  val clientHandler = Akka.system.actorOf(Props[ClientHandler])
  Akka.system.scheduler.schedule(0.seconds, 60.seconds, clientHandler, CleanOutdatedClient)

  val playlist = Akka.system.actorOf(Props[Playlist])
  val songPicker = Akka.system.actorOf(Props[SongPicker])
  val chatLogger = Akka.system.actorOf(Props[ChatLogger])
  val userHandler = Akka.system.actorOf(Props[UserHandler])
  val neo4j = Akka.system.actorOf(Props[Neo4j])

  //date
  def getDate() = new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date())

  //index
  def index = Action {
    Ok(views.html.index()).withHeaders("X-Frame-Options" -> "DENY")
  }

  //sync playback
  def sync = Action.async { request =>
    (playlist ? CurrentSong).mapTo[String].map(str => Ok(str))
  }

  //websocket
  def ws = WebSocket.acceptWithActor[String, String] {
    request => out => Props(classOf[Client], out)
  }

  //
  def chat = Action(parse.json) { request =>
    val json = request.body
    val name = (json \ "name").as[String]
    require(name.replaceAll("\\s", "") != "")
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
        <strong style="color: gold" title={ checkedID }>{ name }</strong>
        <p title={ getDate() }>{
          //decorate the url
          msg.split(" ").map { s =>
            try {
              new URL(s)
              <a target="_blank" href={ s }>{ s }</a>
            } catch {
              case e: MalformedURLException => " " + s + " "
            }
          }
        }</p>.mkString

      //log the message to chatroom
      clientHandler ! BroadcastJson(Json.obj("type" -> "chat", "content" -> chatLog))
      //notification
      clientHandler ! BroadcastJson(Json.obj(
        "type" -> "notify",
        "title" -> name,
        "body" -> msg,
        "tag" -> "chat"))
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

  //= = = backend = = =
  def backend = Action {
    Ok(views.html.backend())
  }

  def statusError = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    val token = (json \ "token").as[String]

    val userId = (userHandler ? InspectToken(token)).mapTo[String]
    userId.filter(_ == "628930919").flatMap(_ => {
      getStoredInfo(GetNodes(Labels.song, "status", "error"))
    }).recover { case _ => BadRequest }
  }

  def updateVideoId = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    val token = (json \ "token").as[String]
    val nodeId = (json \ "nodeId").as[String].toLong
    val newVideoId = (json \ "newVideoId").as[String]

    val userId = (userHandler ? InspectToken(token)).mapTo[String]
    userId.filter(_ == "628930919").map(_ => {
      neo4j ! SetProperties(nodeId, Seq("videoId" -> newVideoId, "status" -> "OK"))
      Ok(newVideoId)
    }).recover { case _ => BadRequest }
  }

  def songById = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    val token = (json \ "token").as[String]
    val videoId = (json \ "videoId").as[String]

    val userId = (userHandler ? InspectToken(token)).mapTo[String]
    userId.filter(_ == "628930919").flatMap(_ => {
      getStoredInfo(GetNodes(Labels.song, "videoId", videoId))
    }).recover { case _ => BadRequest }
  }

  def songByTitle = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    val token = (json \ "token").as[String]
    val originTitle = (json \ "originTitle").as[String]

    val userId = (userHandler ? InspectToken(token)).mapTo[String]
    userId.filter(_ == "628930919").flatMap(_ => {
      getStoredInfo(GetNodes(Labels.song, "originTitle", originTitle))
    }).recover { case _ => BadRequest }
  }

  private def getStoredInfo(query: GetNodes) = {
    (neo4j ? query)
      .mapTo[GetNodesResponse]
      .flatMap(resp => {
        (neo4j ? GetProperties(resp.ids, Seq("originTitle", "videoId", "status")))
          .mapTo[GetPropertiesResponse]
          .map(resp => {
            val res = resp.nodes.map(node => {
              val videoId = node.kv.get("videoId").get
              <ul id={ node.id.toString }>
                <li>{ node.kv.get("originTitle").get }</li>
                <li class="video-id">
                  <a href={ "https://www.youtube.com/watch?v=" + videoId } target="_blank">{ videoId }</a>
                </li>
                <li><input/><button class="update">update</button></li>
              </ul>.toString()
            }).mkString
            Ok(res)
          })
      })
  }

}