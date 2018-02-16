package vocaradio

sealed trait MessageWrapper

case class IncomingMessage(
  msg: VocaMessage,
  socketId: String,
  userIdOpt: Option[String]
) extends MessageWrapper

case class OutgoingMessage(
  msg: VocaMessage,
  socketIdOpt: Option[String]
) extends MessageWrapper
