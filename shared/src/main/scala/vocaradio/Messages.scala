package vocaradio

sealed trait MsgIn
case object Join extends MsgIn
case object Leave extends MsgIn

sealed trait MsgOut
case object TurnOnPlayerControl extends MsgOut