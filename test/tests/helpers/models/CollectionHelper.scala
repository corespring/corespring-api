package tests.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission

object CollectionHelper {

  import faker._

  def create(organizationId: ObjectId): ObjectId =
    ContentCollection.insertCollection(orgId = organizationId, ContentCollection(id = new ObjectId, name = Company.name), Permission.Write) match {
      case Right(contentCollection) => contentCollection.id
      case _ => throw new Exception("Failed to create collection")
    }

  def delete(collectionId: ObjectId) = ContentCollection.delete(collectionId)

}
