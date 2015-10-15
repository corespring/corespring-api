package org.corespring.models

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission

/**
 * A ContentCollection
 * ownerOrgId is not a one-to-many with organization. Collections may be in multiple orgs, but they have one 'owner'
 *
 */
case class ContentCollection(
  name: String,
  ownerOrgId: ObjectId,
  isPublic: Boolean = false,
  id: ObjectId = new ObjectId())

/**
 * Additional information about the content collection and the given orgs access to it.
 */
case class CollectionInfo(contentCollection: ContentCollection,
  itemCount: Long,
  orgId: ObjectId,
  orgPermission: Permission)

object ContentCollection {
  val Default = "default"
}

