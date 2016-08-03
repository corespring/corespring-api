package org.corespring.itemSearch

import play.api.Logger
import play.api.libs.json.{ JsArray, Json }

import scala.concurrent.{ Await, Future }
import scalaz.{ Failure, Success, Validation }

class AggregateType(
  service: ItemIndexService,
  indexResult: ItemIndexService => (Seq[String] => Future[Validation[Error, Map[String, String]]])) {

  import scala.concurrent.duration._

  private val TIMEOUT = 10.seconds
  private val logger = Logger(classOf[AggregateType])

  def all(collections: Seq[String] = Seq.empty) = {

    val result = Await.result(indexResult(service)(collections), TIMEOUT)

    logger.debug(s"val=all, result=$result")
    result match {
      case Success(keyValues) => {
        val result = JsArray(keyValues.toSeq.map {
          case (key, value) => Json.obj("key" -> key, "value" -> value)
        })
        logger.debug(s"val=all, out=$result")
        result
      }
      case Failure(e) => throw e
    }
  }
}

object AggregateType {
  class ItemType(service: ItemIndexService) extends AggregateType(service, (s) => s.componentTypes)
  class WidgetType(service: ItemIndexService) extends AggregateType(service, (s) => s.widgetTypes)
}