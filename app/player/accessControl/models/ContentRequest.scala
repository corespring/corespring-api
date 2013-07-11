package player.accessControl.models

import controllers.auth.Permission
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

case class ContentRequest(id:ObjectId,p:Permission)

case class VersionedContentRequest(id:VersionedId[ObjectId], p:Permission)
