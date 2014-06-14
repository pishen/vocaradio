package controllers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.actorRef2Scala

class Client(out: ActorRef) extends Actor {
  
  var name = ""
  var lastUpdate = System.currentTimeMillis()
  
  Application.clientHandler ! AddClient(self)

  override def postStop() = {
    Application.clientHandler ! RemoveClient(self)
  }

  def receive = {
    case msgFromClient: String => {
      name = msgFromClient
      lastUpdate = System.currentTimeMillis()
      Application.clientHandler ! BroadcastClientStatus
    }
    case Send(msgToClient) => out ! msgToClient
    case GetClientName => sender ! name
    case StopIfOutdated => if(System.currentTimeMillis() - lastUpdate > 60000){
      self ! PoisonPill
    }
  }
}

case class Send(msgToClient: String)
case object GetClientName
case object StopIfOutdated