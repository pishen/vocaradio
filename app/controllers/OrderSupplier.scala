package controllers

import akka.actor.Actor
import scalax.io.Resource
import models.MusicStore
import scala.util.Random
import java.util.Date

class OrderSupplier extends Actor {
  private val titles = Resource.fromFile("titles")
  private val groupSize = 25
  
  private var groups = makeGroups()
  private var start = currentSecs()
  
  def receive = {
    case CurrentGroup => {
      
    }
    case RandomPick =>
    case p: Pick =>
  }
  
  private def currentGroup() = {
    val elapse = currentSecs() - start
    if(elapse >= 3600){
      updateGroup()
      (groups.head, 0L)
    }else{
      (groups.head, elapse)
    }
  }
  
  private def updateGroup() = {
    if(groups.size == 1) groups = makeGroups()
    else groups = groups.tail
    //TODO pick the songs already in playlist
    start = currentSecs()
  }
  
  private def makeGroups() = {
    Random.shuffle(titles.lines().map(SongHolder.apply)).grouped(groupSize).init
  }
  
  case class SongHolder(originTitle: String) {
    lazy val song = MusicStore.getSong(originTitle)
    def htmlString = ""
  }
}

case object CurrentGroup
case object RandomPick
case class Pick(originTitle: String, userName: String, userID: String)
