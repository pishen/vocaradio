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
        case ((state, _), wrapped) =>
          logger.info(wrapped.toString)
          wrapped.msg match {
            case Join =>
              state -> List.empty[WrappedMsgOut]
            case Leave =>
              state -> List.empty[WrappedMsgOut]
          }
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
}
