package vocaradio

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{
  FormData,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  Uri
}
import akka.stream.Materializer
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object HttpHelpers {
  implicit class RichUri(uri: Uri) {
    def withQuery(query: (String, String)*) = {
      uri.withQuery(Uri.Query(query: _*))
    }
    def get()(implicit system: ActorSystem, materializer: Materializer) = {
      Http().singleRequest(HttpRequest(uri = uri))
    }
    def getJson(timeout: FiniteDuration = 30.second)(
        implicit system: ActorSystem,
        materializer: Materializer,
        ec: ExecutionContext
    ) = {
      get().flatMap(
        _.entity.toStrict(timeout).map(_.data.utf8String).map(Json.parse)
      )
    }
    def post(data: (String, String)*)(
        implicit system: ActorSystem,
        materializer: Materializer
    ) = {
      Http().singleRequest(
        HttpRequest(HttpMethods.POST, uri, entity = FormData(data: _*).toEntity)
      )
    }
  }
}
