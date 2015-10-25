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

  //revealed songs for order
  var playlistA: Seq[Song] = Seq.empty

  //hidden buffer list
  var playlistB: Seq[String] = Seq.empty
  
  //counter for error
  var numOfNoSong = 0

  def receive = default

  def default: Receive = {
    case GetPlaying =>
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
        context.become(shifting)
        self ! Shift
      }
    case GetPlaylistA =>
      sender ! playlistA
  }

  def shifting: Receive = {
    case Shift =>
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
          .foreach(keys => self ! AllKeys(keys))
      }
    case song: Song =>
      //got a valid song from toPlay
      numOfNoSong = 0
      playlistA :+= song
      if (playlistA.size == 26) {
        //shift and play, keep 25 songs in the buffer
        playing = Some((playlistA.head, System.currentTimeMillis))
        playlistA = playlistA.tail
        //broadcast updatePlaylist message
        hub ! Hub.Broadcast(Json.obj("msg" -> "updatePlaylist"))
        
        unstashAll()
        context.become(default)
      } else {
        //not enough buffering songs
        self ! Shift
      }
    case SongNotFound =>
      numOfNoSong += 1
      if (numOfNoSong < 26) {
        self ! Shift
      } else {
        Logger.error("Keep getting SongNotFound, stop shifting, player is dead.")
      }
    case AllKeys(keys) =>
      //refill
      val remainingKeys = playlistB.toSet
      playlistB = playlistB ++ Random.shuffle(keys.filterNot(remainingKeys.contains))
      self ! Shift
    case _ => stash()
  }
}

object Player {
  def props(songBase: ActorRef, hub: ActorRef)(implicit ws: WSClient) = Props(new Player(songBase, hub))
  //message from Application
  case object GetPlaying
  case object GetPlaylistA
  //message for internal usage
  case object Shift
  case object SongNotFound
  case class AllKeys(keys: Seq[String])
}
