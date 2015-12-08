package filters

import play.api.Logger
import play.api.mvc.SimpleResult

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }

trait FutureQueuer {
  def queued(key: String)(body: => Future[SimpleResult]): Future[SimpleResult]
}

class BlockingFutureQueuer(implicit val ec: ExecutionContext) extends FutureQueuer {

  private val results: mutable.Map[String, Future[SimpleResult]] = mutable.Map()

  private val logger = Logger(classOf[BlockingFutureQueuer])

  /**
   * Invoke the fn once for a given key and whilst that future is computing  return that [[Future[SimpleResult]]],
   * to subsequent requests to the same key
   * This method is synchronized to ensure that the map is populated by the first concurrent thread
   * and the remaining threads can use that thread's Future[SimpleResult]
   */

  def queued(key: String)(body: => Future[SimpleResult]): Future[SimpleResult] = synchronized {
    logger.debug(s"function=queued, key=$key, result=${results.get(key)}")

    val out = results.get(key).getOrElse {
      val f = body.map { r =>
        logger.trace(s"function=queued, key=$key - result=$r, remove future from results map")
        results.remove(key)
        r
      }

      logger.trace(s"function=queued, key=$key - put future into results map")
      results.put(key, f)
      f
    }
    logger.debug(s"function=queued, out=$out")
    out
  }
}
