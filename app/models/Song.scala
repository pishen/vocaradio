package models

case class Song(originTitle: String, videoId: String, title: String, duration: Int)

object Song {
  //TODO think a better way to do this
  def error(originTitle: String) = Song(originTitle, "error", "", 0)
}