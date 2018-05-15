package vocaradio

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("YT.Player")
class Player(id: String, config: js.Object) extends js.Object {
  def cueVideoById(videoId: String): Unit = js.native
  def loadVideoById(videoId: String, startSeconds: Int): Unit = js.native
  def getVideoData(): VideoData = js.native
  def getCurrentTime(): Double = js.native
  def seekTo(seconds: Int, allowSeekAhead: Boolean): Unit = js.native
}

@js.native
trait VideoData extends js.Object {
  val video_id: String = js.native
}

@js.native
@JSGlobal("YT.PlayerState")
object PlayerState extends js.Any {
  val ENDED: Int = js.native
  val PLAYING: Int = js.native
}
