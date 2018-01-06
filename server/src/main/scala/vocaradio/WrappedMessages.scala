package vocaradio

case class WrappedMsgIn(
  msg: MsgIn,
  socketId: String,
  userIdOpt: Option[String]
)

case class WrappedMsgOut(
  msg: MsgOut,
  socketIdOpt: Option[String]
)
