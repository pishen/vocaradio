package vocaradio

sealed trait VocaMessage

case object Join extends VocaMessage
case object Leave extends VocaMessage

case class UserStatus(isLoggedIn: Boolean, isAdmin: Boolean) extends VocaMessage
