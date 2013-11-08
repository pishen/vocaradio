package controllers

import akka.actor.Actor
import ChatLogger._
import play.api.libs.json.Json

class ChatLogger extends Actor {
  var logs = Seq.empty[Log]
  
  def receive = {
    case l: Log => {
      logs +:= l
      if(logs.length > 100) logs = logs.init
    }
    case GetHistory => sender ! logs
  }
}

object ChatLogger {
  case class Log(user: String, msg: String){
    def toJsObj = Json.obj("user" -> user, "msg" -> msg)
  }
  case object GetHistory
}