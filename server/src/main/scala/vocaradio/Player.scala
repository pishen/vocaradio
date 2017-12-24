package vocaradio

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging

object Player extends LazyLogging {
  case class PlayerState()

  def createSinkAndSource()(implicit materializer: ActorMaterializer) =
    MergeHub
      .source[WrappedMsgIn]
      .scan(
        PlayerState() -> List.empty[WrappedMsgOut]
      ) {
        case ((state, _), msg) =>
          logger.info(msg.toString)
          state -> List.empty[WrappedMsgOut]
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
}
