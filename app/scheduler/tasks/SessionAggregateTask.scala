package scheduler.tasks

import akka.util.duration._
import controllers.Log
import akka.util.Duration
import models.itemSession.ItemSession
import com.mongodb.casbah.Imports._
import play.api.Logger


class SessionAggregateTask extends RabbitMQTask{
  override val initialDelay = Duration.Zero
  override val frequency = 1 minute

  def run() {
    Logger.info("running session aggregate task at "+System.currentTimeMillis())
//    val mapJS:JSFunction = """
//      function () {
//          emit(this.itemId,this)
//      }
//                """
//    ItemSession.collection.mapReduce()
  }
}
