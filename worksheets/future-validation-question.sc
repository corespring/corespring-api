import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaz.{Failure, Success, Validation}


object FutureValidation {
  def apply[E, A](validation: => Validation[E, A])(implicit executor: ExecutionContext): FutureValidation[E, A] =
    apply(Future(validation))
}

case class FutureValidation[+E, +A](futval: Future[Validation[E, A]]) {
  def map[B](f: A => B)(implicit executor: ExecutionContext): FutureValidation[E, B] = {
    val result = futval.map { validation =>
      validation.fold(
        fail => Failure(fail),
        succ => Success(f(succ))
      )
    }
    FutureValidation(result)
  }

  def flatMap[EE >: E, B](f: A => FutureValidation[EE, B])(implicit executor: ExecutionContext): FutureValidation[EE, B] = {
    val result = futval flatMap { validation =>
      validation.fold(
        fail => Future(Failure(fail)),
        succ => f(succ).futval
      )
    }
    FutureValidation(result)
  }
}




def getName : FutureValidation[Int,String] = {
  FutureValidation(Future{ Failure(11) } )
}

def useName(name:String) : FutureValidation[Int,String] = {
  FutureValidation(Future{ Success(s"hi $name") })
}


//val name : Future[Validation[Int,String]] = getName.flatMap{ v =>
//  v match {
//    case Success(name) => useName(name)
//    case Failure(e) => Future(Failure(e))
//  }
//}
val result = for{
  name <- getName
  b <- useName(name)
} yield b

/* name is now a Validation[Int,String]
 * can I make the for comprehension run within the Future > Validation context?
    val result = for{
      name <- getName
      b <- useName(name)
    } yield b
*/

Await.result(result.futval, 1.second)
