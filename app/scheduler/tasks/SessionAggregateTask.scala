package scheduler.tasks

import scala.concurrent.duration._
import org.corespring.common.log.PackageLogging

//TODO: Find out whats supposed to happen here.
class SessionAggregateTask extends RabbitMQTask with PackageLogging {
  override val initialDelay = Duration.Zero
  override val frequency = 1.minute

  def run() {
    logger.error("This run function does nothing")
  }
}
