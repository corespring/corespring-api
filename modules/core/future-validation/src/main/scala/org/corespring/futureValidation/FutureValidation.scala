package org.corespring.futureValidation

import scala.concurrent.{Future, ExecutionContext}
import scalaz.{Success, Failure, Validation}

/**
  * http://stackoverflow.com/questions/34356470/working-with-futurevalidatione-a-in-for-comprehensions
  */

object FutureValidation {
  def apply[E, A](validation: => Validation[E, A])(implicit executor: ExecutionContext): FutureValidation[E, A] = {
    apply(Future(validation))
  }
}

case class FutureValidation[+E, +A](future: Future[Validation[E, A]]) {
  def map[B](f: A => B)(implicit executor: ExecutionContext): FutureValidation[E, B] = {
    val result = future.map { validation =>
      validation.fold(
        fail => Failure(fail),
        success => Success(f(success))
      )
    }
    FutureValidation(result)
  }

  def flatMap[EE >: E, B](f: A => FutureValidation[EE, B])(implicit executor: ExecutionContext): FutureValidation[EE, B] = {
    val result = future.flatMap { validation =>
      validation.fold(
        fail => Future(Failure(fail)),
        success => f(success).future
      )
    }
    FutureValidation(result)
  }
}

