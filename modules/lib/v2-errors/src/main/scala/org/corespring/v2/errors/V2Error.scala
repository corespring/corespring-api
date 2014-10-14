package org.corespring.v2.errors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ JsObject, Json, JsValue }
import play.api.mvc.RequestHeader
import play.api.http.Status._

/**
 * Base type for all known V2 errors.
 * @param message
 * @param statusCode
 */
sealed abstract class V2Error(val message: String, val statusCode: Int = BAD_REQUEST) {

  def errorType: String = this.getClass.getSimpleName

  def json: JsObject = Json.obj("message" -> message, "errorType" -> errorType)

}

case class Field(name: String, fieldType: String)

sealed abstract class identificationFailed(rh: RequestHeader, msg: String = "Failed to identify an organization for request") extends V2Error(s"${rh.path} - $msg", UNAUTHORIZED)

private[v2] object Errors {

  case class missingRequiredField(fields: Field*) extends V2Error(s"Missing the following required field(s): ${fields.map(f => s"${f.name} : ${f.fieldType}").mkString(", ")}")

  case class encryptionFailed(msg: String) extends V2Error(s"encryption failed: $msg", BAD_REQUEST)

  case class permissionNotGranted(msg: String) extends V2Error(msg, UNAUTHORIZED)

  case class compoundError(msg: String, errs: Seq[V2Error], override val statusCode: Int) extends V2Error(msg, statusCode) {
    override def json: JsObject = super.json ++ Json.obj("subErrors" -> errs.map(_.json))
  }

  case class invalidQueryStringParameter(badName: String, expectedName: String) extends V2Error(s"Bad query string parameter name: $badName - you should be using $expectedName")

  case class noApiClientAndPlayerTokenInQueryString(rh: RequestHeader) extends identificationFailed(rh, "No 'apiClient' and 'playerToken' in queryString")

  case class noToken(rh: RequestHeader) extends identificationFailed(rh, "No access token")

  case class noUserSession(rh: RequestHeader) extends identificationFailed(rh, "No user session")

  case class invalidToken(rh: RequestHeader) extends identificationFailed(rh, "Invalid access token")

  case class expiredToken(rh: RequestHeader) extends identificationFailed(rh, "Expired access token")

  case class noOrgForToken(rh: RequestHeader) extends identificationFailed(rh, "No organization for access token")

  case class noDefaultCollection(orgId: ObjectId) extends V2Error(s"No default collection defined for org ${orgId}")

  case class generalError(msg: String, override val statusCode: Int = BAD_REQUEST) extends V2Error(msg, statusCode)

  case object notReady extends V2Error("not ready")

  case object noJson extends V2Error("No json in request body")

  case class invalidJson(str: String) extends V2Error(s"Invalid json $str")

  case object errorSaving extends V2Error("Error saving")

  case object needJsonHeader extends V2Error("You need to set the Content-Type to 'application/json'")

  case class propertyNotFoundInJson(name: String) extends V2Error(s"can't find $name in request body")

  case class noOrgIdAndOptions(request: RequestHeader) extends V2Error(s"can not load orgId and PlayerOptions from session: ${request.session.data}", UNAUTHORIZED)

  case class noCollectionIdForItem(vid: VersionedId[ObjectId]) extends V2Error(s"This item has no collection id $vid")

  case class invalidCollectionId(collectionId: String, itemId: VersionedId[ObjectId]) extends V2Error(s"invalid collectionId in item $collectionId in item ${itemId}")

  case class orgCantAccessCollection(orgId: ObjectId, collectionId: String, accessType: String) extends V2Error(s"The org: $orgId can't access ($accessType) collection: $collectionId")

  case object default extends V2Error("Failed to grant access")

  case class cantLoadSession(id: String) extends V2Error(s"Can't load session with id $id", NOT_FOUND)

  case class noItemIdInSession(id: String) extends V2Error(s"no item id in session: $id")

  case class cantParseItemId(id: String) extends V2Error(s"Can't parse itemId: $id")

  case class cantFindItemWithId(vid: VersionedId[ObjectId]) extends cantFindById("item", vid.toString())

  case class cantFindOrgWithId(orgId: ObjectId) extends cantFindById("org", orgId.toString)

  abstract class cantFindById(name: String, id: String) extends V2Error(s"Can't find $name with id $id", NOT_FOUND)

  case class invalidPval(pval: Long, collectionId: String, orgId: ObjectId) extends V2Error(s"Unknown pval: $pval for collection $collectionId in org: $orgId")

  case class errorSaving(msg: String = "Error Saving") extends V2Error(msg)

  case class incorrectJsonFormat(badJson: JsValue) extends V2Error(s"Bad json format ${Json.stringify(badJson)}")

  case class orgDoesntReferToCollection(orgId: ObjectId, collectionId: String)
    extends V2Error(s"org $orgId doesn't refer to collection: $collectionId", UNAUTHORIZED)

  case class inaccessibleItem(itemId: VersionedId[ObjectId], orgId: ObjectId, p: Permission)
    extends V2Error(s"org: $orgId can't access item: $itemId with permission ${p.name}", UNAUTHORIZED)

  case class unAuthorized(errors: String*) extends V2Error(errors.mkString(", "), UNAUTHORIZED)

  case class insufficientPermission(pval: Long, requestedPermission: Permission)
    extends V2Error(s"${Permission.toHumanReadable(pval)} does not allow ${requestedPermission.name}", UNAUTHORIZED)

  case class cantFindSession(id: String) extends V2Error(s"Can't find session with id: $id", NOT_FOUND)

  case class sessionDoesNotContainResponses(sessionId: String) extends V2Error(s"session: $sessionId does not contain any responses")
}
