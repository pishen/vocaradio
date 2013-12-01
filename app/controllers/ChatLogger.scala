package controllers

import akka.actor.Actor
import akka.actor.actorRef2Scala
import scalax.io.Resource

class ChatLogger extends Actor {
  //private val logfile = Resource.fromFile("chat-logs")
  //private var chatLogs = logfile.lines().toSeq.takeRight(100)
  private var chatLogs = Seq.empty[String]

  def receive = {
    case cl: ChatLog => {
      chatLogs :+= cl.content
      //logfile.write(cl.content + "\n")
      if (chatLogs.length > 500) chatLogs = chatLogs.tail
    }
    case GetHistory => sender ! chatLogs
  }
}

case class ChatLog(content: String)
case object GetHistory
