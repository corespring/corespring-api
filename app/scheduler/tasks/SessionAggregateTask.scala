package scheduler.tasks

import scheduler.RabbitMQTask
import akka.util.duration._
import controllers.Log
import akka.util.Duration


class SessionAggregateTask extends RabbitMQTask{
  override val initialDelay = Duration.Zero
  override val frequency = 1 minute

  def run() {
    Log.i("ran session aggregate task at "+System.currentTimeMillis())
  }
}
