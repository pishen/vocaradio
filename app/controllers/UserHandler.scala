package controllers

import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import scalax.io.Resource

class UserHandler extends Actor {
  val appSecret = Resource.fromFile("fb-app-secret").lines().head
  val appID = 565192206888536L
  //mutable
  var validTokens = Map.empty[String, String]

  def receive = {
    case InspectToken(token) => {
      validTokens.get(token) match {
        case Some(id) => sender ! id
        case None => {
          val userID = askFB(token)
          userID.foreach(id => self ! AddTokenUserPair(token -> id))
          userID pipeTo sender
        }
      }
    }
    case AddTokenUserPair(pair) => validTokens += pair
  }

  private def askFB(token: String) = {
    WS.url("https://graph.facebook.com/debug_token")
      .withQueryString(
        "input_token" -> token,
        "access_token" -> (appID + "|" + appSecret))
      .get
      .map(resp => {
        val json = resp.json
        require((json \ "data" \ "app_id").as[Long] == appID)
        require((json \ "data" \ "is_valid").as[Boolean])
        (json \ "data" \ "user_id").as[Long].toString
      })
  }
}

case class InspectToken(token: String)
case class AddTokenUserPair(pair: (String, String))