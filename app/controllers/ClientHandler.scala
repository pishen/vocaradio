package controllers

import scala.concurrent.Future

import Application.timeout
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper

class ClientHandler extends Actor {

  var clients = Set.empty[ActorRef]

  def receive = {
    case AddClient(client) => clients += client
    case RemoveClient(client) => clients -= client
    case Broadcast(msg) => clients.foreach(_ ! Send(msg))
    case BroadcastJson(json) => clients.foreach(_ ! Send(Json.stringify(json)))
    case CleanOutdatedClient => clients.foreach(_ ! StopIfOutdated)
    case BroadcastClientStatus => {
      self ! Broadcast(clientCountMsg)
      nameListMsgF.map(msg => Broadcast(msg)) pipeTo self
    }
  }

  def clientCountMsg = {
    val content =
      <div style="text-align: center">
        <span style="font-size: 40px">{ clients.size }</span>
        listener(s)
      </div>.toString
    Json.stringify(Json.obj("type" -> "clientCount", "content" -> content))
  }

  def nameListMsgF = {
    Future.sequence(clients.map(client => (client ? GetClientName).mapTo[String])).map(names => {
      val content = <div>{ (names - "").mkString(", ") }</div>.toString
      Json.stringify(Json.obj("type" -> "onlineList", "content" -> content))
    })
  }

}

case class AddClient(client: ActorRef)
case class RemoveClient(client: ActorRef)
case class Broadcast(msg: String)
case class BroadcastJson(json: JsValue)
case object CleanOutdatedClient
case object BroadcastClientStatus