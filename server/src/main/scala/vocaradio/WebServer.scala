package vocaradio

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import slick.jdbc.H2Profile.api._
import CirceHelpers._
import H2._
import HttpHelpers._

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

  val fbAppId = conf.getString("facebook.app-id")
  val fbRedirectUri = conf.getString("facebook.redirect-uri")
  val fbAppSecret = conf.getString("facebook.app-secret")

  val goHome = redirect("/", PermanentRedirect)

  val (wsSink, wsSource) = MergeHub.source[String]
    .scan((0, "Welcome!")) { case ((count, _), msg) =>
      println(msg)
      if (msg == "JOIN") {
        (count + 1, s"There are ${count + 1} people in the room.")
      } else if (msg == "LEAVE") {
        (count - 1, s"There are ${count - 1} people in the room.")
      } else {
        (count, msg)
      }
    }
    .map(_._2)
    .toMat(BroadcastHub.sink)(Keep.both).run

  val route = get {
    pathSingleSlash {
      getFromResource("assets/html/index.html")
    } ~ path("login") {
      redirect(
        Uri("https://www.facebook.com/v2.11/dialog/oauth").withQuery(
          "client_id" -> fbAppId,
          "redirect_uri" -> fbRedirectUri
        ),
        PermanentRedirect
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
              Uri("https://graph.facebook.com/v2.11/me")
                .withQuery(
                  "access_token" -> (json \ "access_token").asUnsafe[String]
                )
                .getJson()
                .map(json => (json \ "id").asUnsafe[String])
            }
        }(id => setUserId(id)(goHome))
      } ~ goHome
    } ~ path("logout") {
      clearUserId(goHome)
    } ~ (path("connect")) {
      handleWebSocketMessages {
        Flow.fromSinkAndSource(
          Flow[Message]
            .collect {
              case TextMessage.Strict(msg) => msg
            }
            .prepend(Source.single("JOIN"))
            .++(Source.single("LEAVE"))
            .to(wsSink),
          wsSource.map(msg => TextMessage(msg))
        )
      }
    }
  } ~ pathPrefix("assets") {
    getFromResourceDirectory("assets")
  } ~ (post & requireUserId) { userId =>
    println(userId)
      (path("song") & formFields("query", "id".?)) { (query, id) =>
      complete {
        db.run(songs += Song(query, id)).map(_.toString)
      }
    }
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
