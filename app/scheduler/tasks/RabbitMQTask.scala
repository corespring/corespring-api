package scheduler.tasks

import play.api.libs.json.{JsNull, JsValue}
import scala.concurrent.duration.{FiniteDuration, Duration}

trait RabbitMQTask extends Runnable{
  val initialDelay:FiniteDuration = Duration.Zero
  val frequency:FiniteDuration = Duration.Zero
  var data:JsValue = JsNull
}
