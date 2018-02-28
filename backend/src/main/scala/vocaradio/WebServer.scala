package vocaradio

import java.util.UUID

import scala.concurrent.{Future, Promise}

import CirceHelpers._
import HttpHelpers._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import slick.jdbc.H2Profile.api._
import Global._

object WebServer extends App with LazyLogging {
  implicit val sessionManager = new SessionManager[String](
    SessionConfig.fromConfig()
  )
  def setUserId(userId: String) = setSession(oneOff, usingCookies, userId)
  val requireUserId = requiredSession(oneOff, usingCookies)
  val optionalUserId = optionalSession(oneOff, usingCookies)
  val clearUserId = invalidateSession(oneOff, usingCookies)

  val goHome = redirect("/", TemporaryRedirect)

  val (playerSink, playerSource) = Player.createSinkAndSource()
  val (songbaseSink, songbaseSource) = SongBase.createSinkAndSource()

  val route = get {
    pathSingleSlash {
      getFromResource("assets/html/index.html")
    } ~ path("login") {
      redirect(
        Uri("https://www.facebook.com/v2.11/dialog/oauth").withQuery(
          "client_id" -> fbAppId,
          "redirect_uri" -> fbRedirectUri
        ),
        TemporaryRedirect
      )
    } ~ path("callback") {
      parameters("code") { code =>
        onSuccess {
          Uri("https://graph.facebook.com/v2.11/oauth/access_token")
            .withQuery(
              "client_id" -> fbAppId,
              "redirect_uri" -> fbRedirectUri,
              "client_secret" -> fbAppSecret,
              "code" -> code
            )
            .getJson()
            .flatMap { json =>
              (json \ "access_token").asF[String]
            }
            .flatMap { accessToken =>
              Uri("https://graph.facebook.com/v2.11/me")
                .withQuery("access_token" -> accessToken)
                .getJson()
            }
            .flatMap { json =>
              (json \ "id").asF[String]
            }
        }(id => setUserId(id)(goHome))
      } ~ goHome
    } ~ path("logout") {
      clearUserId(goHome)
    } ~ (path("connect") & optionalUserId) { userIdOpt =>
      val uuid = UUID.randomUUID.toString
      handleWebSocketMessages {
        Flow.fromSinkAndSource(
          Flow[Message]
            .mapAsync(1) {
              case tm: TextMessage =>
                tm.textStream.runReduce(_ + _).map { str =>
                  decode[VocaMessage](str).toOption
                }
              case bm: BinaryMessage =>
                bm.dataStream.runWith(Sink.ignore).map(_ => None)
            }
            .mapConcat(_.toList)
            .prepend(Source.single(Join))
            .++(Source.single(Leave))
            .map(msg => Incoming(msg, uuid, userIdOpt))
            .alsoTo(songbaseSink)
            .to(playerSink),
          playerSource
            .merge(songbaseSource)
            .filter(_.socketIdOpt.map(_ == uuid).getOrElse(true))
            .map(_.msg)
            .prepend(
              Source.single(
                UserStatus(
                  userIdOpt.isDefined,
                  userIdOpt.map(_ == adminId).getOrElse(false)
                )
              )
            )
            .map(_.asJson.noSpaces)
            .map(TextMessage.apply)
        )
      }
    }
  } ~ pathPrefix("assets") {
    getFromResourceDirectory("assets")
  }

  val promise = Promise[String]()
  sys.addShutdownHook {
    promise.success("Shutdown")
  }

  for {
    binding <- Http().bindAndHandle(route, "localhost", 8080)
    _ <- promise.future
    _ <- binding.unbind()
  } yield {
    system.terminate()
  }
}
