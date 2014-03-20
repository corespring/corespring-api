package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission

object CollectionHelper {

  import faker._

  def create(organizationId: ObjectId): ObjectId =
    ContentCollection.insertCollection(orgId = organizationId, ContentCollection(id = new ObjectId, name = Company.name, ownerOrgId = organizationId), Permission.Write, true) match {
      case Right(contentCollection) => contentCollection.id
      case _ => throw new Exception("Failed to create collection")
    }

  def createMultiple(orgId: ObjectId, count: Int = 1, clear: Boolean = true): Seq[ObjectId] = {

    if (clear) {
      ContentCollection.collection.dropCollection()
    }

    (1 to count).map { i =>
      create(orgId)
    }
  }

  /**
   * Provides ObjectIds for all public collections
   */
  def public: Seq[ObjectId] = {
    ContentCollection.getPublicCollections match {
      case stream: Stream[ContentCollection] => stream.foldLeft(Seq.empty[ObjectId])({ (acc, collection) => acc :+ collection.id })
      case seq: Seq[ContentCollection] => seq.map(_.id)
    }
  }

  def delete(collectionIds: ObjectId*) = {
    collectionIds.foreach {
      ContentCollection.delete(_)
    }
  }

}
