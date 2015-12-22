import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

def getMessage = Future("hi")
def fail = Future.failed(new RuntimeException("Ouch"))


val out = for{
  msg <- getMessage
  r <- fail
} yield r

val recovered = out.recoverWith({
  case r : RuntimeException => Future(r.getMessage)
  case t : Throwable => Future("Oh that was sore")
})
Await.result(recovered, 1.second)