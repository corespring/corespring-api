package org.corespring.models

import com.mongodb.casbah.Imports._

/**
 * A ContentCollection
 * ownerOrgId is not a one-to-many with organization. Collections may be in multiple orgs, but they have one 'owner'
 *
 */
case class ContentCollection(
    name: String,
    ownerOrgId: ObjectId,
    isPublic: Boolean = false,
    id: ObjectId = new ObjectId()) {

  //TODO: RF: itemCount is now on ContentCollectionService
}

object ContentCollection{
  val Default = "default"
}

case class CollectionExtraDetails(coll: ContentCollection, access: Long)

