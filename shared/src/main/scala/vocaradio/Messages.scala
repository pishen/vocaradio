package vocaradio

sealed trait MsgIn
case object Join extends MsgIn
case object Leave extends MsgIn
case class AddSong(query: String, id: Option[String]) extends MsgIn

sealed trait MsgOut
case class UserStatus(isLoggedIn: Boolean, isAdmin: Boolean) extends MsgOut
