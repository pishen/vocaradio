package controllers

import akka.actor.Actor
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.Json

class Broadcaster extends Actor {
  private val (enumerator, channel) = Concurrent.broadcast[String]
  private var clientCount = 0
  
  def receive = {
    case Join => {
      clientCount += 1
      sender ! enumerator
    }
    case Quit => clientCount -= 1
    case BCClientCount => channel.push(clientCountMsg)
    case ToAll(msg) => channel.push(msg)
  }
  
  def clientCountMsg = {
    Json.stringify(Json.obj("type" -> "clientCount", "content" -> clientCount))
  }
}

case object Join
case object Quit
case object BCClientCount
case class ToAll(msg: String)
