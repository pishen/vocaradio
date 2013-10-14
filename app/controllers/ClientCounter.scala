package controllers

import akka.actor.Actor

class ClientCounter extends Actor {
  private var clientCount = 0
  
  def receive = {
    case ClientCounter.AddClient => clientCount += 1
    case ClientCounter.RemoveClient => clientCount -= 1
    case ClientCounter.Count => sender ! clientCount
  }
}

object ClientCounter {
  case object AddClient
  case object RemoveClient
  case object Count
}