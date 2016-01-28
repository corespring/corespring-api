package bootstrap

import akka.actor.{ Actor, Props }
import org.bson.types.ObjectId
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.platform.data.mongo.models.VersionedId
import play.libs.Akka

object Actors {

  def itemTransformerActor(transformer: ItemTransformer) = Akka.system.actorOf(Props.create(classOf[ItemTransformerActor], transformer))

  case class UpdateItem(itemId: VersionedId[ObjectId])

  class ItemTransformerActor(transformer: ItemTransformer) extends Actor {
    override def receive: Actor.Receive = {
      case UpdateItem(itemId) => {
        transformer.updateV2Json(itemId)
      }
    }
  }

}

