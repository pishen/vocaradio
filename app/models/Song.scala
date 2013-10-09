package models

class Song(val id: String, val duration: Int)

object Song {
  def apply(id: String, duration: Int) = new Song(id, duration)
}