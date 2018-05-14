package vocaradio

import java.util.UUID

import scala.concurrent.{Future, Promise}

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
import slick.jdbc.meta.MTable

object Main extends App with LazyLogging {
  // database
  db.run(MTable.getTables).foreach { tables =>
    if (tables.isEmpty) {
      db.run(SongBase.songs.schema.create)
        .failed
        .foreach(t => logger.error("Failed on creating songs table", t))
    }
  }

  // session management
  implicit val sessionManager = new SessionManager[String](
    SessionConfig.fromConfig()
  )
  def setUserId(userId: String) = setSession(oneOff, usingCookies, userId)
  val requireUserId = requiredSession(oneOff, usingCookies)
  val optionalUserId = optionalSession(oneOff, usingCookies)
  val clearUserId = invalidateSession(oneOff, usingCookies)

  // redirects
  def goto(uri: Uri) = redirect(uri, TemporaryRedirect)
  val goHome = redirect("/", TemporaryRedirect)

  // hubs
  val (playerSink, playerSource) = Player.createSinkAndSource()
  val (songbaseSink, songbaseSource) = SongBase.createSinkAndSource()

  val route = get {
    pathSingleSlash {
      getFromResource("assets/html/index.html")
    } ~ path("login") {
      goto(Facebook.loginPage)
    } ~ path("callback") {
      parameters("code") { code =>
        //TODO toTry.get here?
        onSuccess(Facebook.getUser(code).map(_.toTry.get)) {
          id => setUserId(id)(goHome)
        }
      }
    } ~ path("logout") {
      clearUserId(goHome)
    } ~ (path("connect") & optionalUserId) { userIdOpt =>
      val uuid = UUID.randomUUID.toString
      handleWebSocketMessages {
        Pipe.clientFlow(
          uuid,
          userIdOpt,
          Flow[Pipe.In].alsoTo(songbaseSink).to(playerSink),
          playerSource.merge(songbaseSource)
        )
      }
    }
  } ~ pathPrefix("assets") {
    getFromResourceDirectory("assets")
  }

  // TODO: any simpler solution?
  val promise = Promise[String]()
  sys.addShutdownHook {
    promise.success("Shutdown")
  }

  for {
    binding <- Http().bindAndHandle(route, "localhost", httpPort)
    _ <- promise.future
    _ <- binding.unbind()
  } yield {
    system.terminate()
  }
}
