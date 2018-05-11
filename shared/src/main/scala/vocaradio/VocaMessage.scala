package vocaradio

sealed trait VocaMessage

case class UserStatus(
  isLoggedIn: Boolean,
  isAdmin: Boolean
) extends VocaMessage

case class AddSong(
  query: String,
  id: Option[String]
) extends VocaMessage

case class SongAdded(query: String) extends VocaMessage

case object Ready extends VocaMessage
case class Resume(id: String, position: Int) extends VocaMessage
case object Ended extends VocaMessage

case class Load(id: String) extends VocaMessage
case class Play(id: String, position: Int) extends VocaMessage

case class Picker(name: String, userIdOpt: Option[String]) {
  def cleanUserId() = this.copy(userIdOpt = None)
}
case class Pickable(video: Video, pickerOpt: Option[Picker]) {
  def cleanUserId() = this.copy(pickerOpt = pickerOpt.map(_.cleanUserId()))
}
case class UpdatePlaylist(pickables: Seq[Pickable]) extends VocaMessage
