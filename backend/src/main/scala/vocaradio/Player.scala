package vocaradio

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging

object Player extends LazyLogging {
  case class PlayerState()

  def createSinkAndSource()(implicit materializer: ActorMaterializer) = {
    MergeHub
      .source[IncomingMessage]
      .scan(
        PlayerState() -> List.empty[OutgoingMessage]
      ) {
        case ((state, _), wrapped) =>
          wrapped.msg match {
            case Join =>
              logger.info(wrapped.toString)
              state -> List.empty[OutgoingMessage]
            case Leave =>
              logger.info(wrapped.toString)
              state -> List.empty[OutgoingMessage]
            case _ =>
              state -> List.empty[OutgoingMessage]
          }
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
  }
}
