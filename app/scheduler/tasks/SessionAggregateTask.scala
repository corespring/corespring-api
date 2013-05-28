package scheduler.tasks

import akka.util.Duration
import akka.util.duration._
import common.log.PackageLogging

//TODO: Find out whats supposed to happen here.
class SessionAggregateTask extends RabbitMQTask with PackageLogging{
  override val initialDelay = Duration.Zero
  override val frequency = 1 minute

  def run() {
    Logger.error("This run function does nothing")
  }
}
