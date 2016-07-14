package org.corespring.models.appConfig

import org.bson.types.ObjectId

case class Bucket(bucket: String)
case class ArchiveConfig(contentCollectionId: ObjectId, orgId: ObjectId)
case class AccessTokenConfig(tokenDurationInHours: Int = 24)
case class DefaultOrgs(v2Player: Seq[ObjectId], root: ObjectId)

case class AllowExpiredTokens(value: Boolean = false)
