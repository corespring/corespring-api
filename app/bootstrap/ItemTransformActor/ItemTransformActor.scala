package bootstrap

import akka.actor.{ Actor, Props }
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.libs.Akka

object Actors {

  lazy val itemTransformerActor = Akka.system.actorOf(Props.create(classOf[ItemTransformerActor], Main.itemTransformer))

  case class UpdateItem(itemId: VersionedId[ObjectId])

  class ItemTransformerActor(transformer: ItemTransformer) extends Actor {
    override def receive: Actor.Receive = {
      case UpdateItem(itemId) => {
        transformer.updateV2Json(itemId)
      }
    }
  }

}

