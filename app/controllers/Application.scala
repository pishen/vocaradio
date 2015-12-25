package controllers

import java.util.UUID
import javax.inject._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.Play.current
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import Player._
import net.ceedubs.ficus.Ficus._
import better.files._

@Singleton
class Application @Inject() (implicit ws: WSClient, system: ActorSystem) extends Controller {
  implicit val timeout = Application.timeout
  
  val config = com.typesafe.config.ConfigFactory.load()
  val oauth2ClientId = config.as[String]("google.oauth2.id")
  val oauth2RedirectUri = config.as[String]("google.oauth2.redirect")
  val oauth2Secret = config.as[String]("google.oauth2.secret")
  val admins = config.as[Seq[String]]("admins")
  val backgrounds = config.as[Seq[String]]("backgrounds")

  val hub = system.actorOf(Props[Hub], "hub")
  val songBase = system.actorOf(Props[SongBase], "songBase")
  val player = system.actorOf(Player.props(songBase, hub), "player")
  val chatLogger = system.actorOf(ChatLogger.props(hub), "chatLogger")

  //Google OAuth2 https://developers.google.com/identity/protocols/OAuth2WebServer
  object Authenticated extends AuthenticatedBuilder({
    request => request.session.get("user")
  }, {
    request => Unauthorized
  })

  object AuthenticatedAdmin extends AuthenticatedBuilder({
    request => request.session.get("user").filter(admins.contains)
  }, {
    request => Unauthorized
  })

  def login = Action { request =>
    request.session.get("state") match {
      case Some(state) =>
        Redirect("https://accounts.google.com/o/oauth2/auth", Map(
          "response_type" -> "code",
          "client_id" -> oauth2ClientId,
          "redirect_uri" -> oauth2RedirectUri,
          "scope" -> "email",
          "state" -> state
        ).mapValues(v => Seq(v)))
      case None =>
        BadRequest("Don't take the shortcut.")
    }
  }

  def callback(code: Option[String], state: Option[String]) = Action.async { request =>
    if (state.flatMap(remoteState => request.session.get("state").map(_ == remoteState)).getOrElse(false)) {
      code match {
        case Some(code) =>
          ws.url("https://www.googleapis.com/oauth2/v3/token")
            .post(Map(
              "code" -> code,
              "client_id" -> oauth2ClientId,
              "client_secret" -> oauth2Secret,
              "redirect_uri" -> oauth2RedirectUri,
              "grant_type" -> "authorization_code"
            ).mapValues(v => Seq(v)))
            .map(resp => (resp.json \ "access_token").as[String])
            .flatMap { token =>
              ws.url("https://www.googleapis.com/plus/v1/people/me")
                .withHeaders("Authorization" -> s"Bearer $token")
                .get()
                .map(_.json)
                .map { json =>
                  val email = (json \ "emails" \\ "value").head.as[String]
                  Redirect("/").withSession("user" -> email)
                }
            }
        case None =>
          Future.successful(Redirect("/"))
      }
    } else {
      Future.successful(BadRequest("state not matched."))
    }
  }

  def logout = Action {
    Redirect("/").withNewSession
  }

  //\\//\\main//\\//\\
  def index = Action { request =>
    val verification = request.session.get("user") match {
      case None =>
        <div style="height:34px">
          <span>Not connected.</span><a class="btn btn-primary pull-right" href="/login">Login with Google</a>
        </div>
      case Some(user) =>
        <div style="height:34px">
          <span>{ user } </span><a class="btn btn-default pull-right" href="/logout">Logout</a>
        </div>
    }
    Ok(views.html.index(verification, Random.shuffle(backgrounds).head))
      .withSession(request.session + ("state" -> UUID.randomUUID.toString))
  }

  //TODO move the json wrapper of playing into Player, let it possible to broadcast the json to client by websocket
  def playing = Action.async {
    (player ? GetPlaying).mapTo[(Song, Long)].map {
      case (song, playedSeconds) => Ok(Json.obj("song" -> song, "playedSeconds" -> playedSeconds))
    }
  }

  def request = Authenticated(parse.urlFormEncoded) { request =>
    val reqMap = request.body.mapValues(_.head)
    //TODO handle exception here
    val id = reqMap("id")
    val name = reqMap("name")
    if (name != "") {
      player ! Player.Request(id, Requester(request.user, name))
      Ok
    } else {
      Unauthorized("name cannot be empty")
    }
  }
  
  def chat = Authenticated(parse.urlFormEncoded) { request =>
    val reqMap = request.body.mapValues(_.head)
    val opt = for {
      name <- reqMap.get("name").filter(_ != "")
      text <- reqMap.get("text").filter(_ != "")
    } yield {
      chatLogger ! ChatLogger.Chat(name, text, System.currentTimeMillis, admins.contains(request.user))
      Ok("got it")
    }
    opt.getOrElse(BadRequest)
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    Client.props(out, hub, player, chatLogger)
  }

  //\\backend//\\
  def backend = AuthenticatedAdmin {
    Ok(views.html.backend())
  }

  def shift = AuthenticatedAdmin {
    player ! Shift
    Ok
  }

  def kick(id: String) = AuthenticatedAdmin {
    player ! Kick(id)
    Ok
  }

  private def buildBackendTable(seq: Seq[(String, String)]) = {
    val rows = seq.map {
      case (key, id) =>
        <tr><td class="key">{ key }</td><td>{ id }</td></tr>
    }
    <table class="table">
      <thead><tr><td>key</td><td>video id</td></tr></thead>
      <tbody>{ rows }</tbody>
    </table>
  }

  def getNotFounds = AuthenticatedAdmin.async {
    (songBase ? SongBase.GetNotFounds).mapTo[Seq[(String, String)]]
      .map(buildBackendTable)
      .map(table => Ok(table.toString))
  }

  def getSongByKey(key: String) = AuthenticatedAdmin.async {
    (songBase ? SongBase.GetSong(key)).mapTo[Song]
      .map(song => Seq((key, song.id)))
      .map(buildBackendTable)
      .map(table => Ok(table.toString))
  }

  def getSongsById(id: String) = AuthenticatedAdmin.async {
    (songBase ? SongBase.GetSongById(id)).mapTo[Seq[String]]
      .map(keys => keys.map(_ -> id))
      .map(buildBackendTable)
      .map(table => Ok(table.toString))
  }

  def setSongId = AuthenticatedAdmin(parse.urlFormEncoded) { request =>
    val reqMap = request.body.mapValues(_.head)
    val key = reqMap("key")
    val newId = reqMap("newId")
    YouTubeAPI.getSong(newId).foreach(song => songBase ! SongBase.SetSong(key, song))
    Ok
  }

  def mergeKeys = AuthenticatedAdmin(parse.multipartFormData) { request =>
    val file = request.body.file("keys").get.ref.file
    val keys = file.toScala.lines.map(key => if (key.endsWith(".mp4")) key.dropRight(4) else key).toSet
    songBase ! SongBase.MergeKeys(keys)
    Ok
  }
}

object Application {
  val timeout = Timeout(20.seconds)
}
