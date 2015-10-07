package controllers

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.ws.WSClient
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import Player._
import SongBase._
import scala.util.Failure
import scala.util.Random
import scala.util.Success

class Player(songBase: ActorRef)(implicit ws: WSClient) extends Actor with Stash {
  implicit val timeout = Timeout(5.seconds)

  //Option[(Song, start time(ms))]
  var playing: Option[(Song, Long)] = None

  //revealed songs for order
  var toPlaySoon: Seq[Song] = Seq.empty

  //hidden buffer list
  var toPlay: Seq[String] = Seq.empty

  def receive = default

  def default: Receive = {
    case GetPlaying =>
      val currentTime = System.currentTimeMillis()
      val songIsPlaying = playing.map {
        case (song, startTime) => startTime + song.seconds * 1000 > currentTime
      }.getOrElse(false)

      if (songIsPlaying) {
        //return (song, current position(sec))
        sender ! playing.map {
          case (song, startTime) => (song, (currentTime - startTime) / 1000)
        }.get
      } else {
        //shifting mode
        stash()
        context.become(shifting)
        self ! Shift
      }
    case GetToPlaySoon =>
      sender ! toPlaySoon
  }

  def shifting: Receive = {
    case Shift =>
      if (toPlay.size > 300) {
        //try to make the first key from toPlay to a Song
        val key = toPlay.head
        toPlay = toPlay.tail

        (songBase ? GetSong(key)).mapTo[Song]
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
              songBase ! SetSong(key, Some(song))
            case Failure(e) =>
              //shift one more and set this song as fail in songbase
              self ! Shift
              songBase ! SetSong(key, None)
          }
      } else {
        //get keys for refilling
        (songBase ? GetAllKeys).mapTo[Seq[String]]
          .foreach(keys => self ! AllKeys(keys))
      }
    case song: Song =>
      //got a valid song from toPlay
      toPlaySoon :+= song
      if (toPlaySoon.size == 26) {
        //shift and play, keep 25 songs in the buffer
        playing = Some((toPlaySoon.head, System.currentTimeMillis))
        toPlaySoon = toPlaySoon.tail
        unstashAll()
        context.become(default)
      } else {
        //not enough buffering songs
        self ! Shift
      }
    case AllKeys(keys) =>
      //refill
      val remainingKeys = toPlay.toSet
      toPlay = toPlay ++ Random.shuffle(keys.filterNot(remainingKeys.contains))
      self ! Shift
    case _ => stash()
  }
}

object Player {
  //message from Application
  case object GetPlaying
  case object GetToPlaySoon
  //message for internal usage
  case object Shift
  case class AllKeys(keys: Seq[String])
}
