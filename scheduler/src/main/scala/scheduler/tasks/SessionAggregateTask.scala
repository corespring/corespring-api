package scheduler.tasks

import scheduler.RabbitMQTask
import akka.util.duration._
import controllers.Log
import akka.util.Duration
import models.itemSession.ItemSession
import com.mongodb.casbah.Imports._
import models.item.Item
import play.api.libs.json.Json


class SessionAggregateTask extends RabbitMQTask{
  override val initialDelay = Duration.Zero
  override val frequency = 1 minute

  def run() {
    val item = Item.findOne(MongoDBObject())
    println("running session aggregate task at "+System.currentTimeMillis())
    println("got item: "+Json.toJson(item).toString())
//    val mapJS:JSFunction = """
//      function () {
//          emit(this.itemId,this)
//      }
//                """
//    ItemSession.collection.mapReduce()
  }
}
