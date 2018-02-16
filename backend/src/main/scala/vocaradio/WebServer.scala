package vocaradio

import java.util.UUID

import scala.concurrent.{Future, Promise}

import CirceHelpers._
import HttpHelpers._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import cats.implicits._
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

object WebServer extends App with LazyLogging {
  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("vocaradio")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val sessionManager =
    new SessionManager[String](SessionConfig.fromConfig())

  def setUserId(userId: String) = setSession(oneOff, usingCookies, userId)
  val requireUserId = requiredSession(oneOff, usingCookies)
  val optionalUserId = optionalSession(oneOff, usingCookies)
  val clearUserId = invalidateSession(oneOff, usingCookies)

  val adminId = conf.getString("admin-id")

  val fbAppId = conf.getString("facebook.app-id")
  val fbRedirectUri = conf.getString("facebook.redirect-uri")
  val fbAppSecret = conf.getString("facebook.app-secret")

  val goHome = redirect("/", TemporaryRedirect)

  val (playerSink, playerSource) = Player.createSinkAndSource()
  // val songbaseSink = Flow[WrappedMsgIn]
  //   .takeWhile(_.userIdOpt.map(_ == adminId).getOrElse(false))
  //   .map(_.msg)
  //   .to {
  //     Sink.foreach {
  //       case add: AddSong =>
  //         H2.addSong(add)
  //       case _ => //do nothing
  //     }
  //   }

  H2.init()

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
          val res = for {
            accessTokenJson <- Uri
              .apply("https://graph.facebook.com/v2.11/oauth/access_token")
              .withQuery(
                "client_id" -> fbAppId,
                "redirect_uri" -> fbRedirectUri,
                "client_secret" -> fbAppSecret,
                "code" -> code
              )
              .getJson()
              .asEitherT
            accessToken <- accessTokenJson
              .hcursor
              .downField("access_token")
              .as[String]
              .asEitherT[Future]
            meJson <- Uri("https://graph.facebook.com/v2.11/me")
              .withQuery("access_token" -> accessToken)
              .getJson()
              .asEitherT
            id <- meJson
              .hcursor
              .downField("id")
              .as[String]
              .asEitherT[Future]
          } yield id
          res.value.map(_.toTry.get)
        }(id => setUserId(id)(goHome))
      } ~ goHome
    } ~ path("logout") {
      clearUserId(goHome)
    } ~ (path("connect") & optionalUserId) { userIdOpt =>
      val uuid = UUID.randomUUID.toString
      handleWebSocketMessages {
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
          .map(msg => IncomingMessage(msg, uuid, userIdOpt))
          .alsoTo(playerSink)
          .mapConcat {
            case _ => List.empty[OutgoingMessage]
          }
          .merge(playerSource)
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
