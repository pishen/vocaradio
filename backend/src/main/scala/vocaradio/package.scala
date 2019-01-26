import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import cats.data._
import cats.implicits._
import com.typesafe.config.ConfigFactory
import io.circe._
import io.circe.parser._
import scala.concurrent.Future
import scala.concurrent.duration._
import slick.jdbc.H2Profile.api._

package object vocaradio {
  implicit val system = ActorSystem("vocaradio")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val conf = ConfigFactory.load()
  val httpPort = conf.getInt("http.port")
  val adminId = conf.getString("admin-id")
  val fbAppId = conf.getString("facebook.app-id")
  val fbRedirectUri = conf.getString("facebook.redirect-uri")
  val fbAppSecret = conf.getString("facebook.app-secret")
  val googleApiKey = conf.getString("google.api-key")

  val db = Database.forConfig("h2")

  case class UriDecodeException(
      uri: Uri,
      resp: String,
      error: Error
  ) extends Exception

  implicit class RichUri(uri: Uri) {
    def withQuery(query: (String, String)*) = {
      uri.withQuery(Uri.Query(query: _*))
    }
    def get[T: Decoder] = {
      Http()
        .singleRequest(HttpRequest(uri = uri))
        .flatMap {
          _.entity.toStrict(5.second).map(_.data.utf8String).map { str =>
            decode[T](str).left
              .map(error => UriDecodeException(uri, str, error))
          }
        }
    }
  }
}
