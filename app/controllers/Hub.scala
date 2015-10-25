package controllers

import akka.actor._
import Hub._
import play.api.libs.json._

class Hub extends Actor {
  
  //Map[actor, (name, lastUpdate)]
  var clients = Map.empty[ActorRef, (String, Long)]
  
  //TODO set a timer to clean quiet clients
  //send PoisonPill to them
  
  
  
  def receive = {
    case AddClient(client, name) =>
      clients += (client -> (name -> System.currentTimeMillis))
      //TODO broadcast client list if it's new client or name is changed
    case RemoveClient(client) =>
      clients -= client
      //TODO broadcast client list
    case Broadcast(json) =>
      clients.keys.foreach(_ ! Client.Send(json))
  }
}

object Hub {
  case class AddClient(client: ActorRef, name: String)
  case class RemoveClient(client: ActorRef)
  case class Broadcast(json: JsValue)
}
