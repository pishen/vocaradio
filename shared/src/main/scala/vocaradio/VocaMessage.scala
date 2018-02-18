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
