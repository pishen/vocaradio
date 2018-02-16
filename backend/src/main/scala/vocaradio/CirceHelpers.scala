package vocaradio

import scala.concurrent.Future
import scala.language.higherKinds

import cats._
import cats.data.EitherT
import io.circe._

object CirceHelpers {
  implicit class RichFutureResult[T](result: Future[Either[Error, T]]) {
    def asEitherT = EitherT(result)
  }

  implicit class RichResult[T](result: Either[Error, T]) {
    def asEitherT[F[_]: Applicative] = EitherT.fromEither[F](result)
  }
}
