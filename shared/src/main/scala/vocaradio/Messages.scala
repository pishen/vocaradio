package vocaradio

sealed trait MsgIn
case class Join(uuid: String, id: Option[String]) extends MsgIn
case class Leave(uuid: String) extends MsgIn

sealed trait MsgOut
case class A() extends MsgOut
