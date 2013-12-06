package models

case class Song(originTitle: String, videoId: String, title: String, duration: Int)

object Song {
  def error(originTitle: String) = Song(originTitle, "error", "error", 0)
}