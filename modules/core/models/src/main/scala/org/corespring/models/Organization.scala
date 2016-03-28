package org.corespring.models

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission

case class Organization(name: String,
  path: Seq[ObjectId] = Seq(),
  contentcolls: Seq[ContentCollRef] = Seq(),
  metadataSets: Seq[MetadataSetRef] = Seq(),
  displayConfig: DisplayConfig = DisplayConfig.default,
  id: ObjectId = new ObjectId()) {

  def accessibleCollections = contentcolls.filter(_.enabled).flatMap { r =>
    Permission.fromLong(r.pval).filter {
      p => p.has(Permission.Read)
    }.map(_ => r)
  }

}

case class ContentCollRef(collectionId: ObjectId, pval: Long = Permission.Read.value, enabled: Boolean = false)

case class MetadataSetRef(metadataId: ObjectId, isOwner: Boolean)

