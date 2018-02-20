package vocaradio

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import Global._

object HttpHelpers extends LazyLogging {
  implicit class RichUri(uri: Uri) {
    def withQuery(query: (String, String)*) = {
      uri.withQuery(Uri.Query(query: _*))
    }
    def getJson(timeout: FiniteDuration = 30.second) = {
      Http()
        .singleRequest(HttpRequest(uri = uri))
        .flatMap(
          _.entity.toStrict(timeout).map(_.data.utf8String).map {
            str => parse(str).toTry.get
          }
        )
    }
  }
}
