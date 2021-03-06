package vocaradio

import akka.NotUsed
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object Pipe {
  sealed trait PipeMessage
  sealed trait SystemMessage extends PipeMessage
  case object Join extends SystemMessage
  case object Leave extends SystemMessage

  case class ClientMessage(value: VocaMessage) extends PipeMessage

  case class In(
    msg: PipeMessage,
    socketId: String,
    userIdOpt: Option[String]
  )

  // TODO: New way to specify target?
  case class Out(
    msg: VocaMessage,
    targetSocketOpt: Option[String]
  )

  def clientFlow(
    socketId: String,
    userIdOpt: Option[String],
    sinkHub: Sink[In, NotUsed],
    sourceHub: Source[Out, NotUsed]
  ) = {
    val sink = Flow[Message]
      .mapAsync(1) {
        case tm: TextMessage =>
          tm.textStream.runReduce(_ + _).map { str =>
            decode[VocaMessage](str).toOption
          }
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore).map(_ => None)
      }
      .mapConcat(_.toList)
      .map(msg => ClientMessage(msg))
      .prepend(Source.single(Join))
      .++(Source.single(Leave))
      .map(msg => In(msg, socketId, userIdOpt))
      .to(sinkHub)

    // TODO: cut the client when it is too slow
    val source = sourceHub
      .filter(_.targetSocketOpt.map(_ == socketId).getOrElse(true))
      .map(_.msg)
      .prepend(
        Source.single(
          UserStatus(
            userIdOpt.isDefined,
            userIdOpt.map(_ == adminId).getOrElse(false)
          )
        )
      )
      .map(_.asJson.noSpaces)
      .map(TextMessage.apply)

    Flow.fromSinkAndSourceCoupled(sink, source)
  }
}
