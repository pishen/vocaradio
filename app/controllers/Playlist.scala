package controllers

import Application.orderSupplier
import akka.actor.Actor
import akka.actor.actorRef2Scala
import models.Song
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper

class Playlist extends Actor {
  private val lowerBound = 20

  private var queue = Seq.empty[Order]
  private var playing: Option[(Order, Long)] = None

  for (i <- 1 to lowerBound) orderSupplier ! RandomPick

  def receive = {
    case Playing => {
      //check whether update to next song
      if (needNext && queue.nonEmpty) playNext()
      //reply
      playing match {
        case None => sender ! "empty queue"
        case Some((order, startTime)) => {
          order.song match {
            case None => sender ! ("broken song: " + order.originTitle)
            case Some(v) => {
              val json = Json.obj("id" -> v.videoId,
                "position" -> (currentTime - startTime),
                "userName" -> order.userName)
              sender ! Json.stringify(json)
            }
          }
        }
      }
    }
    case AddOrder(order) => {
      val (s1, s2) = queue.span(_.userID != "voca")
      queue = s1 ++ Seq(order) ++ s2
      val index = queue.indexOf(order)
      if(index == queue.indices.last){
        broadcast(Json.obj("type" -> "updatePlaylist", "append" -> orderToHtml(order)))
      }else{
        broadcast(Json.obj("type" -> "updatePlaylist", "insert" -> index, "content" -> orderToHtml(order)))
      }
    }
    case RemoveOrder(ot) => //TODO not impl yet
    case Queue => {
      val json = Json.obj("content" -> queue.map(orderToHtml).mkString)
      sender ! Json.stringify(json)
    } 
  }

  private def needNext = {
    playing match {
      case None => true
      case Some((order, startTime)) => {
        order.song match {
          case None    => true
          case Some(v) => currentTime - startTime > v.duration - 1
        }
      }
    }
  }

  private def playNext() = {
    val nextOrder = queue.head
    playing = Some((nextOrder, currentTime))
    queue = queue.tail
    broadcast(Json.obj("type" -> "updatePlaylist", "remove" -> "0"))
    if (nextOrder.userID == "voca") orderSupplier ! RandomPick
  }

  private def orderToHtml(order: Order) = {
    order.song match {
      case Some(v) => {
        <span>
          <img src={ v.thumbDefault } title={ v.title + "\nordered by: " + order.userName }/>
        </span>.toString
      }
      case None => <span></span>.toString
    }
  }

}

case object Playing
case object Queue
case class Order(originTitle: String, userName: String, userID: String, song: Option[Song])
case class AddOrder(order: Order)
case class RemoveOrder(originTitle: String)