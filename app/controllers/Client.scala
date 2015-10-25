package controllers

import akka.actor._
import Client._
import play.api.libs.json._

class Client(out: ActorRef, hub: ActorRef) extends Actor {
  
  override def postStop() = {
    hub ! Hub.RemoveClient(self)
  }
  
  def receive = {
    case name: String =>
      hub ! Hub.AddClient(self, name)
    case Send(json) =>
      out ! Json.stringify(json)
  }
}

object Client {
  def props(out: ActorRef, hub: ActorRef) = Props(new Client(out, hub))
  
  case class Send(json: JsValue)
}
