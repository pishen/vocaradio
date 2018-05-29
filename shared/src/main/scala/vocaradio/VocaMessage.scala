package vocaradio

sealed trait VocaMessage

case object Ready extends VocaMessage
case class Resume(id: String, position: Int) extends VocaMessage
case object Ended extends VocaMessage

// // // // //

case class UserStatus(
  isLoggedIn: Boolean,
  isAdmin: Boolean
) extends VocaMessage

case class Load(id: String) extends VocaMessage
case class Play(id: String, position: Int) extends VocaMessage

case class Picker(name: String, userIdOpt: Option[String]) {
  def cleanUserId() = this.copy(userIdOpt = None)
}
case class Pickable(video: Video, pickerOpt: Option[Picker]) {
  def cleanUserId() = this.copy(pickerOpt = pickerOpt.map(_.cleanUserId()))
}
case class UpdatePlaylist(pickables: Seq[Pickable]) extends VocaMessage

// // // // //

case object Drop extends VocaMessage
case class Skip(id: String) extends VocaMessage

case class Save(song: Song) extends VocaMessage
case class BatchSave(song: Song) extends VocaMessage
case class Delete(query: String) extends VocaMessage

case class Id(value: String) extends VocaMessage
case class Query(value: String) extends VocaMessage

// // // // //

case class Saved(query: String) extends VocaMessage
case class BatchSaved(query: String) extends VocaMessage
case class Deleted(query: String) extends VocaMessage
case class ShowSongs(songs: Seq[Song]) extends VocaMessage
