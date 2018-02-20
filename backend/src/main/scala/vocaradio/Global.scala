package vocaradio

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import slick.jdbc.H2Profile.api._

object Global {
  implicit val system = ActorSystem("vocaradio")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val conf = ConfigFactory.load()
  val adminId = conf.getString("admin-id")
  val fbAppId = conf.getString("facebook.app-id")
  val fbRedirectUri = conf.getString("facebook.redirect-uri")
  val fbAppSecret = conf.getString("facebook.app-secret")
  val googleApiKey = conf.getString("google.api-key")

  val db = Database.forConfig("h2")
}
