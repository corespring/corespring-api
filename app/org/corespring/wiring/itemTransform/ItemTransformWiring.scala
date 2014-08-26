package org.corespring.wiring.itemTransform

import akka.actor.{Actor, Props}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.player.AllItemVersionTransformer
import play.libs.Akka

object ItemTransformWiring {

  lazy val itemTransformer = new AllItemVersionTransformer

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
