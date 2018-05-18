package vocaradio

import akka.NotUsed
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object Pipe {
  sealed trait SysMsg
  case object Join extends SysMsg
  case object Leave extends SysMsg

  case class In(
    // TODO: Use type instead of Either
    msg: Either[SysMsg, VocaMessage],
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
      .map(msg => Right(msg))
      .prepend(Source.single(Left(Join)))
      .++(Source.single(Left(Leave)))
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
