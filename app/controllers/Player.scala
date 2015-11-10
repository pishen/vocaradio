package controllers

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.ws.WSClient
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import Player._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import play.api.Logger

class Player(songBase: ActorRef, hub: ActorRef)(implicit ws: WSClient) extends Actor with Stash {
  implicit val timeout = Timeout(15.seconds)

  //playing ++ playlistA ++ playlistB

  //Option[(Song, start time(ms))]
  var playing: Option[(Song, Long)] = None

  //revealed songs for request
  var playlistA: Seq[SongWithRequester] = Seq.empty

  //hidden buffer list
  var playlistB: Seq[String] = Seq.empty

  //counter for error
  var numOfNoSong = 0

  def buildPlaylistAJson() = Json.toJson(playlistA.map(_.json))
  
  def requestSongFromB() = {
    if (playlistB.size > 300) {
      //try to make the first key from toPlay to a Song
      val key = playlistB.head
      playlistB = playlistB.tail

      (songBase ? SongBase.GetSong(key)).mapTo[Song]
        .flatMap { song =>
          //check if the song is still valid
          YouTubeAPI.getSong(song.id)
        }
        .recoverWith {
          case e: NoSuchElementException =>
            //search for the song if the song is invalid or we don't have song with this key
            YouTubeAPI.searchSong(key).flatMap(id => YouTubeAPI.getSong(id))
        }
        .onComplete {
          case Success(song) =>
            //send the song back to self and set the new song info for songbase
            self ! song
            songBase ! SongBase.SetSong(key, song)
          case Failure(e) =>
            //shift one more and set this song as fail in songbase
            self ! SongNotFound
            songBase ! SongBase.SongNotFound(key)
        }
    } else {
      //get keys for refilling
      (songBase ? SongBase.GetAllKeys).mapTo[Seq[String]]
        .foreach(keys => self ! RefillB(keys))
    }
  }

  def receive = receiver("normal")

  //mode can be normal, shifting, kicking
  def receiver(mode: String): Receive = {
    case GetPlaying =>
      if (mode != "normal") {
        stash()
      } else {
        val currentTime = System.currentTimeMillis()
        val songIsPlaying = playing.map {
          case (song, startTime) => startTime + song.seconds * 1000 > currentTime
        }.getOrElse(false)

        if (songIsPlaying) {
          //return (song, played seconds)
          sender ! playing.map {
            case (song, startTime) => (song, (currentTime - startTime) / 1000)
          }.get
        } else {
          //shifting mode
          stash()
          context.become(receiver("shifting"))
          requestSongFromB()
        }
      }
    case GetPlaylistA =>
      sender ! buildPlaylistAJson()
    case Request(id, requester) if mode == "normal" =>
      playlistA.find(sw => sw.song.id == id && sw.requesterOpt.isEmpty).foreach {
        case SongWithRequester(song, _) =>
          val indexToSplit = playlistA.lastIndexWhere(_.requesterOpt.map(_.email == requester.email).getOrElse(false)) + 1
          val (l, r) = playlistA.splitAt(indexToSplit)
          val (rl, rr) = r.zipWithIndex.span {
            case (SongWithRequester(_, Some(other)), i) =>
              !r.take(i).flatMap(_.requesterOpt).map(_.email).contains(other.email)
            case (SongWithRequester(_, None), _) =>
              false
          }
          playlistA = (l ++ rl.map(_._1) :+ SongWithRequester(song, Some(requester))) ++ rr.map(_._1).filterNot(_.song.id == id)
          hub ! Hub.Broadcast(Client.Send("updatePlaylist", buildPlaylistAJson))
      }
    case song: Song if sender == self =>
      def broadcastAndBecomeNormal() = {
        hub ! Hub.Broadcast(Client.Send("updatePlaylist", buildPlaylistAJson))
        context.become(receiver("normal"))
        unstashAll()
      }
      //got a valid song from toPlay
      numOfNoSong = 0
      playlistA :+= SongWithRequester(song, None)
      mode match {
        case "shifting" if playlistA.size == 26 =>
          //shift and play, keep 25 songs in the buffer
          playing = Some((playlistA.head.song, System.currentTimeMillis))
          playlistA = playlistA.tail
          broadcastAndBecomeNormal()
        case "kicking" if playlistA.size == 25 =>
          broadcastAndBecomeNormal()
        case _ =>
          //not enough buffering songs
          requestSongFromB()
      }
    case SongNotFound if sender == self =>
      numOfNoSong += 1
      if (numOfNoSong < 26) {
        requestSongFromB()
      } else {
        Logger.error("Keep getting SongNotFound, stop shifting, player is dead.")
      }
    case RefillB(keys) if sender == self =>
      //refill
      val remainingKeys = playlistB.toSet
      playlistB = playlistB ++ Random.shuffle(keys.filterNot(remainingKeys.contains))
      //TODO danger, could get infinite loop here
      requestSongFromB()
    case Shift if mode == "normal" =>
      //force player into shifting mode
      context.become(receiver("shifting"))
      requestSongFromB()
      //force all the client to get the playing song again
      hub ! Hub.Broadcast(Client.Send("play", Json.obj()))
    case Kick(id) if mode == "normal" =>
      if (playlistA.exists(_.song.id == id)) {
        context.become(receiver("kicking"))
        playlistA = playlistA.filterNot(_.song.id == id)
        requestSongFromB()
      }
    case _ =>
    //do nothing
  }
}

object Player {
  def props(songBase: ActorRef, hub: ActorRef)(implicit ws: WSClient) = Props(new Player(songBase, hub))
  //helper classes
  case class SongWithRequester(song: Song, requesterOpt: Option[Requester]) {
    val json = {
      val xml =
        <div style={ s"background-image:url('${song.thumbnail}');background-size:cover;" }>
          <div class="overlay">
            <a class="yt-link plain" href={ s"http://www.youtube.com/watch?v=${song.id}" } target="_blank">
              { song.title }
            </a>
            {
              requesterOpt match {
                case None => <button class="request btn btn-default btn-sm">Request</button>
                case Some(requester) => <span class="request">{ requester.name }</span>
              }
            }
          </div>
        </div>
      Json.obj("id" -> song.id, "html" -> xml.toString)
    }
  }
  case class Requester(email: String, name: String)
  //message from Application
  case object GetPlaying
  case object GetPlaylistA
  case class Request(id: String, requester: Requester)
  //message for internal usage
  case object SongNotFound
  case class RefillB(keys: Seq[String])
  //message for admin
  case object Shift
  case class Kick(id: String)
}