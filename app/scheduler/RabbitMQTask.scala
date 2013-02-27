package scheduler

import akka.util.Duration
import play.api.libs.json.JsValue

trait RabbitMQTask extends Runnable{
  val initialDelay:Duration = Duration.Zero
  val frequency:Duration = Duration.Zero
  val data:Option[JsValue] = None
}
