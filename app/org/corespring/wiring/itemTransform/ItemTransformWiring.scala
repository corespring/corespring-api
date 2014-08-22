package org.corespring.wiring.itemTransform

import akka.actor.{ Actor, Props }
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.PlayItemTransformationCache
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.libs.Akka

object ItemTransformWiring {
  lazy val itemTransformer = new ItemTransformer {
    def cache = PlayItemTransformationCache

    def itemService = ItemServiceWired
  }

  lazy val itemTransformerActor = Akka.system.actorOf(Props.create(classOf[ItemTransformerActor], itemTransformer))

  case class UpdateItem(itemId: VersionedId[ObjectId])

  class ItemTransformerActor(transformer: ItemTransformer) extends Actor {
    override def receive: Actor.Receive = {
      case UpdateItem(itemId) => {
        transformer.updateV2Json(itemId)
      }
    }
  }

}
