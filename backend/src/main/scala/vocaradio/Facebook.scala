package vocaradio

import akka.http.scaladsl.model.Uri
import cats.data.EitherT
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import scala.concurrent.Future

object Facebook extends LazyLogging {
  val loginPage =
    Uri("https://www.facebook.com/v2.11/dialog/oauth").withQuery(
      "client_id" -> fbAppId,
      "redirect_uri" -> fbRedirectUri
    )
  def accessTokenUri(code: String) =
    Uri("https://graph.facebook.com/v2.11/oauth/access_token").withQuery(
      "client_id" -> fbAppId,
      "redirect_uri" -> fbRedirectUri,
      "client_secret" -> fbAppSecret,
      "code" -> code
    )
  def meUri(accessToken: String) =
    Uri("https://graph.facebook.com/v2.11/me").withQuery(
      "access_token" -> accessToken
    )

  case class AccessTokenResp(access_token: String)
  case class MeResp(id: String)

  def getUser(code: String): Future[Option[String]] = {
    EitherT(accessTokenUri(code).get[AccessTokenResp])
      .flatMapF(tokenResp => meUri(tokenResp.access_token).get[MeResp])
      .map(meResp => meResp.id)
      .leftMap { decodeException =>
        logger.error("Failed getting User", decodeException)
      }
      .toOption
      .value
  }
}
