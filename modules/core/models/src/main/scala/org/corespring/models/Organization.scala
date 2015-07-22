package org.corespring.models

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission

case class Organization(name: String,
  path: Seq[ObjectId] = Seq(),
  contentcolls: Seq[ContentCollRef] = Seq(),
  metadataSets: Seq[MetadataSetRef] = Seq(),
  id: ObjectId = new ObjectId(),
  isRoot: Boolean) {

  private def readable = (collection: ContentCollRef) => (collection.pval > 0 && collection.enabled == true)
  def accessibleCollections = contentcolls.filter(readable)
}

case class ContentCollRef(collectionId: ObjectId, pval: Long = Permission.Read.value, enabled: Boolean = false)

case class MetadataSetRef(metadataId: ObjectId, isOwner: Boolean)

