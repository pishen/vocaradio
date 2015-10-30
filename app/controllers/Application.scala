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
import Player._
import net.ceedubs.ficus.Ficus._

@Singleton
class Application @Inject() (implicit ws: WSClient, system: ActorSystem) extends Controller {
  implicit val timeout = Timeout(20.seconds)

  val config = com.typesafe.config.ConfigFactory.load()
  val oauth2ClientId = config.as[String]("google.oauth2.id")
  val oauth2RedirectUri = config.as[String]("google.oauth2.redirect")
  val oauth2Secret = config.as[String]("google.oauth2.secret")

  val hub = system.actorOf(Props[Hub], "hub")
  val songBase = system.actorOf(Props[SongBase], "songBase")
  val player = system.actorOf(Player.props(songBase, hub), "player")

  //Google OAuth2 https://developers.google.com/identity/protocols/OAuth2WebServer
  object AuthAction extends AuthenticatedBuilder({
    request => request.session.get("user")
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
        <div>
          <span>Not connected.</span><a class="btn btn-primary pull-right" href="/login">Login with Google</a>
        </div>
      case Some(user) =>
        <div>
          <span>{ user } </span><a class="btn btn-default pull-right" href="/logout">Logout</a>
        </div>
    }
    Ok(views.html.index(verification))
      .withSession(request.session + ("state" -> UUID.randomUUID.toString))
  }

  def playing = Action.async {
    (player ? GetPlaying).mapTo[(Song, Long)].map {
      case (song, playedSeconds) => Ok(Json.obj("song" -> song, "playedSeconds" -> playedSeconds))
    }
  }

  def playlist = Action.async {
    (player ? GetPlaylistA).mapTo[Seq[Song]].map { playlistA =>
      Ok(Json.toJson(playlistA.map(song => Json.obj("id" -> song.id, "html" -> song.html))))
    }
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    Client.props(out, hub)
  }
  
  def backend = Action {
    Ok
  }
  
  def shift = Action {
    
    Ok
  }
  
  def kick(id: String) = Action {
    Ok
  }
  
  def problemSongs = Action {
    Ok
  }
  
  def changeSongId(fromId: Int, toId: Int) = Action {
    Ok
  }
}
