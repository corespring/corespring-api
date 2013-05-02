package player.accessControl.models

import controllers.auth.Permission
import org.bson.types.ObjectId

case class ContentRequest(id:ObjectId,p:Permission)
