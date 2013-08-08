package player.accessControl.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId


case class ContentRequest(id:ObjectId,p:Permission)

case class VersionedContentRequest(id:VersionedId[ObjectId], p:Permission)
