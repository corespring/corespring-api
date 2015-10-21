package org.corespring.it.helpers

import bootstrap.Main
import org.bson.types.ObjectId
import org.corespring.models.ContentCollection
import org.corespring.services.salat.bootstrap.CollectionNames

import scalaz.Success

object CollectionHelper {

  import faker._

  lazy val service = Main.contentCollectionService
  lazy val mongoCollection = Main.db(CollectionNames.contentCollection)

  def create(organizationId: ObjectId): ObjectId =
    service.insertCollection(
      ContentCollection(id = new ObjectId, name = Company.name, ownerOrgId = organizationId)) match {
        case Success(contentCollection) => contentCollection.id
        case _ => throw new Exception("Failed to create collection")
      }

  def createMultiple(orgId: ObjectId, count: Int = 1, clear: Boolean = true): Seq[ObjectId] = {

    if (clear) {
      mongoCollection.dropCollection()
    }

    (1 to count).map { i =>
      create(orgId)
    }
  }

  /**
   * Provides ObjectIds for all public collections
   */
  def public: Seq[ObjectId] = {
    service.getPublicCollections match {
      case stream: Stream[ContentCollection] => stream.foldLeft(Seq.empty[ObjectId])({ (acc, collection) => acc :+ collection.id })
      case seq: Seq[ContentCollection] => seq.map(_.id)
    }
  }

  def delete(collectionIds: ObjectId*) = {
    collectionIds.foreach {
      service.delete(_)
    }
  }

}
