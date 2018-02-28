package vocaradio

sealed trait MessageWrapper

case class Incoming(
  msg: VocaMessage,
  socketId: String,
  userIdOpt: Option[String]
) extends MessageWrapper

case class Outgoing(
  msg: VocaMessage,
  socketIdOpt: Option[String]
) extends MessageWrapper
