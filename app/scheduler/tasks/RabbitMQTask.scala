package scheduler.tasks

import akka.util.Duration
import play.api.libs.json.{JsNull, JsValue}

trait RabbitMQTask extends Runnable{
  val initialDelay:Duration = Duration.Zero
  val frequency:Duration = Duration.Zero
  var data:JsValue = JsNull
}
