package controllers

import akka.actor._
import akka.pattern.ask
import Client._
import play.api.libs.json._
import Player.SongWithRequester
import scala.concurrent.ExecutionContext.Implicits.global

class Client(out: ActorRef, hub: ActorRef, player: ActorRef, chatLogger: ActorRef) extends Actor {
  implicit val timeout = Application.timeout
  
  (player ? Player.GetPlaylistA).mapTo[JsValue]
    .foreach(json => self ! Send("updatePlaylist", json))
  (chatLogger ? ChatLogger.GetChats).mapTo[JsValue]
    .foreach(json => self ! Send("reloadChat", json))
  
  def receive = {
    case name: String =>
      hub ! Hub.AddClient(self, name)
    case Send(msgType, msg) =>
      out ! Json.stringify(Json.obj("msgType" -> msgType, "msg" -> msg))
  }
  
  override def postStop() = {
    hub ! Hub.RemoveClient(self)
  }
}

object Client {
  def props(out: ActorRef, hub: ActorRef, player: ActorRef, chatLogger: ActorRef) = Props(new Client(out, hub, player, chatLogger))
  
  case class Send(msgType: String, msg: JsValue)
}
