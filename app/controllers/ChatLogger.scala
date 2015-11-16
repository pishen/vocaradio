package controllers

import akka.actor._
import ChatLogger._
import play.api.libs.json._
import com.github.nscala_time.time.Imports._

class ChatLogger(hub: ActorRef) extends Actor {
  var chats = Seq.empty[Chat]

  def receive = {
    case chat: Chat =>
      chats :+= chat
      if (chats.length > 500) chats = chats.tail
      hub ! Hub.Broadcast(Client.Send("appendChat", Json.obj("html" -> chat.html, "user" -> chat.user, "text" -> chat.text)))
    case GetChats =>
      sender ! Json.obj("html" -> chats.map(_.html).mkString)
  }
}

object ChatLogger {
  def props(hub: ActorRef) = Props(new ChatLogger(hub))
  case class Chat(user: String, text: String, time: Long, isAdmin: Boolean) {
    val html = {
      val timeStr = new DateTime(time)
        .withZone(DateTimeZone.forID("Asia/Taipei"))
        .toString("MM-dd HH:mm")
      val xml =
        <div>
          <div>
            <span class="chat-user">{ user }</span>
            {
              if (isAdmin) <span class="label label-default">DJ</span> else <span></span>
            }
            <span class="chat-time">{ timeStr }</span>
          </div>
          <p>{ text }</p>
        </div>
      xml.toString
    }
  }
  case object GetChats
}
