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
          wrapped.msg match {
            case Join =>
              logger.info(wrapped.toString)
              state -> List.empty[WrappedMsgOut]
            case Leave =>
              logger.info(wrapped.toString)
              state -> List.empty[WrappedMsgOut]
            case _ =>
              state -> List.empty[WrappedMsgOut]
          }
      }
      .mapConcat(_._2)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run
}
