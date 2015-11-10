package controllers

import akka.actor._
import Hub._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
//import com.github.nscala_time.time.Imports._

class Hub extends Actor {
  //Map[actor, (name, lastUpdate)]
  var clients = Map.empty[ActorRef, (String, Long)]
  
  //TODO set a timer to clean quiet clients
  //send PoisonPill to them
  val c = context.system.scheduler.schedule(0.second, 5.minutes, self, Cleanup)
  
  private def getStatusJson() = {
    val numOfListeners = clients.size
    val clientNames = clients.values.toSeq.map(_._1).filterNot(_ == "")
    Json.obj("numOfListeners" -> numOfListeners, "clientNames" -> clientNames)
  }
  
  def receive = {
    case AddClient(client, name) =>
      clients += (client -> (name -> System.currentTimeMillis))
      self ! Broadcast(Client.Send("updateStatus", getStatusJson))
    case RemoveClient(client) =>
      clients -= client
      self ! Broadcast(Client.Send("updateStatus", getStatusJson))
    case Broadcast(send) =>
      clients.keys.foreach(_ ! send)
    case GetStatus =>
      sender ! getStatusJson()
    case Cleanup =>
      val currentTime = System.currentTimeMillis
      clients.filter(currentTime - _._2._2 > 5.minutes.toMillis).keys.foreach(_ ! PoisonPill)
  }
  
  override def postStop() = {
    c.cancel()
  }
}

object Hub {
  case class AddClient(client: ActorRef, name: String)
  case class RemoveClient(client: ActorRef)
  case class Broadcast(send: Client.Send)
  case object GetStatus
  case object Cleanup
}
