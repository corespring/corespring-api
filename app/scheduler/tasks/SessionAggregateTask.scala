package scheduler.tasks

import scheduler.RabbitMQTask
import akka.util.duration._

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 2/26/13
 * Time: 11:34 PM
 * To change this template use File | Settings | File Templates.
 */
class SessionAggregateTask extends RabbitMQTask{
  override val initialDelay = 1 minute
  override val frequency = 1 minute

  def run() {

  }
}
