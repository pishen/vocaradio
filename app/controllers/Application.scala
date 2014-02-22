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
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket
import scalax.io.Resource
import play.api.libs.iteratee.Concurrent

object Application extends Controller {
  implicit val timeout = Timeout(5000)
  //actors
  val broadcaster = Akka.system.actorOf(Props[Broadcaster])
  val playlist = Akka.system.actorOf(Props[Playlist])
  val songPicker = Akka.system.actorOf(Props[SongPicker])
  val chatLogger = Akka.system.actorOf(Props[ChatLogger])
  val userHandler = Akka.system.actorOf(Props[UserHandler])
  val neo4j = Akka.system.actorOf(Props[Neo4j])
  //websocket
  val (enumerator, channel) = Concurrent.broadcast[String]

  val sdf = new SimpleDateFormat("MM/dd HH:mm:ss")
  val bgImages = Resource.fromFile("bg-images")

  val playlistSize = 25
  val colSize = 5
  val listHeight = (((playlistSize - 1) / colSize + 1) * 114).toString + "px"

  def index = Action.async {
    val bgInfo = Random.shuffle(bgImages.lines()).head.split(" ")
    val bgUrl = bgInfo.head
    val illus = if (bgInfo.size == 3) {
      <a href={ bgInfo(2) } target="_blank">{ bgInfo(1) }</a>
    } else {
      <span>{ bgInfo(1) }</span>
    }
    (songPicker ? StoreStatus).mapTo[(Int, Date)]
      .map {
        case (length, lastUpdate) =>
          Ok(views.html.index(bgUrl, illus, listHeight, length.toString, sdf.format(lastUpdate)))
            .withHeaders("X-Frame-Options" -> "DENY")
      }

  }

  def sync = Action.async { request =>
    //Logger.info("sync: " + request.remoteAddress)
    (playlist ? CurrentSong).mapTo[String].map(str => Ok(str))
  }

  def ws = WebSocket.async[String] { request =>
    (broadcaster ? Join).mapTo[String].map(id => {
      val in = Iteratee.foreach[String](name => {
        broadcaster ! SetName(id, name)
      }).map(_ => {
        broadcaster ! Quit(id)
      })
      (in, enumerator)
    })
  }

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