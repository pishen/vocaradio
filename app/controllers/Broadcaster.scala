package controllers

import akka.actor.Actor
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.Json
import scala.util.Random

class Broadcaster extends Actor {
  private var names = Map.empty[String, String]

  def receive = {
    case Join => {
      val id = getId()
      names = names + (id -> "")
      sender ! id
    }
    case SetName(id, name) => {
      names = names + (id -> name)
      refresh()
    }
    case Quit(id) => {
      names = names - id
      refresh()
    }
    case ToAll(msg) => Application.channel.push(msg)
  }

  def getId(): String = {
    val id = Random.nextString(10)
    if (names.contains(id)) getId() else id
  }

  def refresh() = {
    Application.channel.push(clientCountMsg)
    Application.channel.push(onlineListMsg)
  }

  def clientCountMsg = {
    val content =
      <span class="num">{ names.size }</span>
      <span> { if (names.size > 1) "listeners" else "listener" }</span>.mkString
    Json.stringify(Json.obj("type" -> "clientCount", "content" -> content))
  }

  def onlineListMsg = {
    val content =
      <div>
        { names.values.filter(_.replaceAll("\\s", "") != "").mkString(", ") }
      </div>.toString
    Json.stringify(Json.obj("type" -> "onlineList", "content" -> content))
  }

}

case object Join
case class SetName(id: String, name: String)
case class Quit(id: String)
case class ToAll(msg: String)
