package controllers

import akka.util.Duration
import akka.util.duration._
import play.api.libs.json.JsValue

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 2/26/13
 * Time: 1:14 AM
 * To change this template use File | Settings | File Templates.
 */

object RabbitMQSchedule{
  def apply(initialDelay:Duration, frequency:Duration, data:Option[JsValue])((data:Option[JsValue]) => Unit):
}
