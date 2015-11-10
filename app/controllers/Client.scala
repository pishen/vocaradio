package controllers

import akka.actor._
import akka.pattern.ask
import Client._
import play.api.libs.json._
import Player.SongWithRequester
import scala.concurrent.ExecutionContext.Implicits.global

class Client(out: ActorRef, hub: ActorRef, player: ActorRef) extends Actor {
  implicit val timeout = Application.timeout
  
  (player ? Player.GetPlaylistA).mapTo[JsValue]
    .foreach(json => self ! Send("updatePlaylist", json))
  
  def receive = {
    case name: String =>
      hub ! Hub.AddClient(self, name)
    case Send(msgType, json) =>
      out ! Json.stringify(Json.obj("msgType" -> msgType, "json" -> json))
  }
  
  override def postStop() = {
    hub ! Hub.RemoveClient(self)
  }
}

object Client {
  def props(out: ActorRef, hub: ActorRef, player: ActorRef) = Props(new Client(out, hub, player))
  
  case class Send(msgType: String, json: JsValue)
}
