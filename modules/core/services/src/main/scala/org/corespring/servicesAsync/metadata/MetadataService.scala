package org.corespring.servicesAsync.metadata

import org.bson.types.ObjectId
import org.corespring.models.metadata.Metadata
import org.corespring.platform.data.mongo.models.VersionedId
import scala.concurrent.Future

trait MetadataService {

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Future[Seq[Metadata]]
}
