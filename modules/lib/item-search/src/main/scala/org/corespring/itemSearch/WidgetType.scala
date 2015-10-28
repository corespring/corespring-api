package org.corespring.itemSearch

import org.corespring.itemSearch.AggregateType.AggregationResult
import play.api.Logger
import play.api.libs.json.{ JsArray, Json }

import scala.concurrent.{ Await, Future }
import scalaz.{ Failure, Success, Validation }

object AggregateType {
  type AggregationResult = Future[Validation[Error, Map[String, String]]]
}

private[itemSearch] class AggregateType(service: ItemIndexService, typeResult: ItemIndexService => AggregationResult) {

  private val logger = Logger(classOf[AggregateType])

  def all = {
    import scala.concurrent.duration._
    val result = Await.result(typeResult(service), 10.seconds)

    logger.debug(s"val=all, result=$result")
    result match {
      case Success(keyValues) => {
        val tuples = keyValues.toSeq.map {
          case (key, value) => Json.obj("key" -> key, "value" -> value)
        }
        val out = JsArray(tuples)
        logger.debug(s"val=all, out=$out")
        out
      }
      case Failure(e) => throw e
    }
  }
}

class ItemType(service: ItemIndexService) extends AggregateType(service, (s) => s.componentTypes)

class WidgetType(service: ItemIndexService) extends AggregateType(service, (s) => s.widgetTypes)
