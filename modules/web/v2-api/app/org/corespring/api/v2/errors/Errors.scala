package org.corespring.api.v2.errors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ Json, JsValue }

sealed abstract class V2ApiError(val code: Int, val message: String)

private[v2] object Errors {

  import play.api.http.Status._

  case class generalError(c: Int, msg: String) extends V2ApiError(c, msg)

  object noJson extends V2ApiError(BAD_REQUEST, "No json in request body")
  object needJsonHeader extends V2ApiError(BAD_REQUEST, "You need to set the Content-Type to 'application/json'")
  case class invalidJson(str: String) extends V2ApiError(BAD_REQUEST, s"Invalid json $str")
  case class noCollectionIdInItem(itemId: VersionedId[ObjectId]) extends V2ApiError(BAD_REQUEST, s"No collectionId in item ${itemId}")
  case class invalidCollectionId(collectionId: String, itemId: VersionedId[ObjectId]) extends V2ApiError(BAD_REQUEST, s"invalid collectionId in item $collectionId in item ${itemId}")
  case class invalidPval(pval: Long, collectionId: String, orgId: ObjectId) extends V2ApiError(BAD_REQUEST, s"Unknown pval: $pval for collection $collectionId in org: $orgId")
  object errorSaving extends V2ApiError(BAD_REQUEST, "Error saving")
  case class incorrectJsonFormat(json: JsValue) extends V2ApiError(BAD_REQUEST, s"Bad json format ${Json.stringify(json)}")

  case class orgDoesntReferToCollection(orgId: ObjectId, collectionId: String) extends V2ApiError(UNAUTHORIZED, s"org $orgId doesn't refer to collection: $collectionId")
  case class inaccessibleItem(itemId: VersionedId[ObjectId], orgId: ObjectId, p: Permission) extends V2ApiError(UNAUTHORIZED, s"org: $orgId can't access item: $itemId with permission ${p.name}")
  case class unAuthorized(errors: String*) extends V2ApiError(UNAUTHORIZED, errors.mkString(", "))
  case class insufficientPermission(pval: Long, requestedPermission: Permission) extends V2ApiError(UNAUTHORIZED, s"${Permission.toHumanReadable(pval)} does not allow ${requestedPermission.name}")

  case class cantFindSession(id: String) extends V2ApiError(NOT_FOUND, s"Can't find session with id: $id")

}
