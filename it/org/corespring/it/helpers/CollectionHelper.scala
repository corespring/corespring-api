package org.corespring.it.helpers

import global.Global.main
import org.bson.types.ObjectId
import org.corespring.models.ContentCollection
import org.corespring.services.salat.bootstrap.CollectionNames

import scalaz.Success

object CollectionHelper {

  import faker._

  lazy val service = main.contentCollectionService
  lazy val mongoCollection = main.db(CollectionNames.contentCollection)

  def create(organizationId: ObjectId): ObjectId = create(organizationId, Company.name)

  def create(organizationId: ObjectId, name: String): ObjectId =
    service.insertCollection(
      ContentCollection(id = new ObjectId, name = name, ownerOrgId = organizationId)) match {
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
