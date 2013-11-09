package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.json.Json
import models.Song
import play.api.mvc.WebSocket
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Concurrent
import controllers.ClientCounter._
import controllers.ChatLogger._
import scala.util.Random

object Application extends Controller {
  implicit val timeout = Timeout(5.seconds)
  val playlist = Akka.system.actorOf(Props[Playlist], "playlist")
  val clientCounter = Akka.system.actorOf(Props[ClientCounter], "clientCounter")
  val chatLogger = Akka.system.actorOf(Props[ChatLogger], "chatLogger")
  val (counterOut, counterChannel) = Concurrent.broadcast[String]
  val (chatOut, chatChannel) = Concurrent.broadcast[String]
  val bgUrls = Seq("http://res.nimg.jp/img/watch_zero/walls/wall_ginza.jpg",
      "http://res.nimg.jp/img/watch_zero/walls/wall_night_cruise.jpg",
      "http://res.nimg.jp/img/watch_zero/walls/wall_kabuki.png",
      "http://res.nimg.jp/img/watch_zero/walls/wall_cloud.jpg",
      "http://res.nimg.jp/img/watch_zero/walls/wall_night.jpg")

  def index = Action {
    Ok(views.html.index(bgUrls(Random.nextInt(bgUrls.length))))
  }

  def sync = Action.async {
    (playlist ? Playlist.AskSong).mapTo[(String, Int, String)].map(t => {
      Ok(Json.obj("id" -> t._1, "start" -> t._2, "originTitle" -> t._3))
    })
  }

  def wsCounter = WebSocket.using[String](request => {
    clientCounter ! AddClient

    def broadcast() =
      (clientCounter ? Count).mapTo[Int]
        .foreach(count => counterChannel.push(Json.stringify(Json.obj("clientCount" -> count))))

    val in = Iteratee.foreach[String](wsMsg => {
      broadcast()
    }).map(_ => {
      clientCounter ! RemoveClient
      broadcast()
    })

    (in, counterOut)
  })
  
  def wsChat = WebSocket.using[String](request => {
    val in = Iteratee.foreach[String](wsMsg => {
      val json = Json.parse(wsMsg)
      val log = Log((json \ "user").as[String], (json \ "msg").as[String])
      chatLogger ! log
      chatChannel.push(Json.stringify(log.toJsObj))
    })

    (in, chatOut)
  })
  
  def chatHistory = Action.async {
    (chatLogger ? GetHistory).mapTo[Seq[ChatLogger.Log]].map(logs => {
      Ok(Json.obj("logs" -> Json.toJson(logs.map(_.toJsObj))))
    })
  }

}