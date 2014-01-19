package controllers

import akka.actor.Actor
import scalax.io.Resource
import controllers.OrderSupplier.SongHolder
import models.MusicStore
import scala.util.Random
import java.util.Date

class OrderSupplier extends Actor {
  private val titles = Resource.fromFile("titles")
  private var groups = makeGroups()
  private var start = getTime()
  
  def receive = {
    case OrderSupplier.Content => {
      
    }
  }
  
  private def currentGroup() = {
    //1hr: 3600000 mini secs
    if(getTime() - start >= 3600) updateGroup()
    groups.head
  }
  
  private def updateGroup() = {
    if(groups.size == 1) groups = makeGroups()
    else groups = groups.tail
    //TODO pick the songs already in playlist
    start = getTime()
  }
  
  private def makeGroups() = {
    Random.shuffle(titles.lines().map(SongHolder(_))).grouped(20)
  }
  
  private def getTime() = new Date().getTime() / 1000
  
}

object OrderSupplier {
  case object Content
  case class Pick()
  case class SongHolder(originTitle: String) {
    lazy val songF = MusicStore.getSong(originTitle)
    lazy val htmlString = ""
    var pickedBy: String = null
  }
}