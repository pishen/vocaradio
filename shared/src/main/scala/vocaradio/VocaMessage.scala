package vocaradio

sealed trait VocaMessage

case object Join extends VocaMessage
case object Leave extends VocaMessage

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
case object Resume extends VocaMessage
case object Ended extends VocaMessage

case class Load(id: String) extends VocaMessage
case class Play(id: String, at: Int) extends VocaMessage
