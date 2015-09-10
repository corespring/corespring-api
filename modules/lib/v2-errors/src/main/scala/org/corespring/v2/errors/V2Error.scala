package org.corespring.v2.errors

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.http.Status._

import scalaz.Failure

/**
 * Base type for all known V2 errors.
 * @param message
 * @param statusCode
 */
sealed abstract class V2Error(val title: String, val description: String, val message: String, val statusCode: Int = BAD_REQUEST) {

  def errorType: String = this.getClass.getSimpleName

  def json: JsObject = Json.obj("message" -> message, "errorType" -> errorType)

  def asFailure: Failure[V2Error, _] = Failure(this)

}

case class Field(name: String, fieldType: String)

sealed abstract class identificationFailed(override val title: String, override val description: String, rh: RequestHeader, msg: String = "Failed to identify an organization for request") extends V2Error(title, description, s"${rh.path} - $msg", UNAUTHORIZED)

private[v2] object Errors {

  case class invalidObjectId(id: String, context: String) extends V2Error("Invalid Object Id", "The provided object id was not valid.", s"Invalid object id: $id, context: $context")

  case class missingRequiredField(fields: Field*) extends V2Error("Missing Required Field", "A field that was required was missing", s"Missing the following required field(s): ${fields.map(f => s"${f.name} : ${f.fieldType}").mkString(", ")}")

  case class encryptionFailed(msg: String) extends V2Error("Encryption Failed", "Attempting to encrypt player options failed.", s"encryption failed: $msg", BAD_REQUEST)

  case class permissionNotGranted(msg: String) extends V2Error("Permission Not Granted", "The requested permission was not granted.", msg, UNAUTHORIZED)

  case class compoundError(msg: String, errs: Seq[V2Error], override val statusCode: Int) extends V2Error("Compound Error", "There were multiple errors processing the request.", msg, statusCode) {
    override def json: JsObject = super.json ++ Json.obj("subErrors" -> errs.map(_.json))
  }

  case class invalidQueryStringParameter(badName: String, expectedName: String) extends V2Error("Invalid Query String", "The provided query string was not valid.", s"Bad query string parameter name: $badName - you should be using $expectedName")

  case class noApiClientAndPlayerTokenInQueryString(rh: RequestHeader) extends identificationFailed("No API Client and Player Token in Query String", "An API client and player token were required in order to perform the operation, but they were not provided in the request.", rh, "No 'apiClient' and 'playerToken' in queryString")

  case class noToken(rh: RequestHeader) extends identificationFailed("No Access Token", "An access token was required to perform the operation, but it was not provided by the request.", rh, "No access token")

  case class noUserSession(rh: RequestHeader) extends identificationFailed("No User Session", "A user session was required to perform the operation, but it was not provided by the request.", rh, "No user session")

  case class invalidToken(rh: RequestHeader) extends identificationFailed("Invalid Access Token", "An access token was provided by the request, but it was not valid.", rh, "Invalid access token")

  case class expiredToken(rh: RequestHeader) extends identificationFailed("Expired Access Token", "An access token was provided by the request, but it has expired.", rh, "Expired access token")

  case class noOrgForToken(rh: RequestHeader) extends identificationFailed("No Organization For Token", "The access token provided by the request is not associated with any known organization.", rh, s"No organization for access token ${rh.getQueryString("access_token")}")

  case class noDefaultCollection(orgId: ObjectId) extends V2Error("No Default Collection", "There was no default collection associated with the organization obtained from the request.", s"No default collection defined for org ${orgId}")

  case class generalError(msg: String, override val statusCode: Int = BAD_REQUEST) extends V2Error("General Error", "There was a general, non-specific error processing the request.", msg, statusCode)

  case class noLongerSupported(msg: String, override val statusCode: Int = NOT_IMPLEMENTED) extends V2Error("No Longer Suported", "The action is no longer supported", msg)

  case object notReady extends V2Error("Not Ready", "?", "not ready")

  case object noJson extends V2Error("No JSON", "The body of the provided request was required to contain JSON, but it was not found.", "No json in request body")

  case class invalidJson(str: String) extends V2Error("Invalid JSON", "The JSON body provided by the request was not valid.", s"Invalid json $str")

  case object errorSaving extends V2Error("Error Saving", "There was an error attempting the requested save operation.", "Error saving")

  case object needJsonHeader extends V2Error("Need JSON Header", "The server expected the request to contain the `Content-Type: application/json` header.", "You need to set the Content-Type to 'application/json'")

  case class propertyNotFoundInJson(name: String) extends V2Error("Property Not Found In JSON", "The request's JSON body was expected to contain the provided property, but it was not fonud.", s"can't find $name in request body")

  case class propertyNotAllowedInJson(name: String, jsonIn: JsValue) extends V2Error("Property Not Allowed In JSON", s"There is a property in the json that is not allowed.", s"property '$name' is not allowed - json: $jsonIn")

  case class noOrgIdAndOptions(request: RequestHeader) extends V2Error("No Organization ID and Options", "The organization id and player options could not be read from the request.", s"can not load orgId and PlayerOptions from session: ${request.session.data}", UNAUTHORIZED)

  case class noCollectionIdForItem(vid: VersionedId[ObjectId]) extends V2Error("No Collection ID for Item", "The item did not have a corresponding collection identifier.", s"This item has no collection id $vid")

  case class invalidCollectionId(collectionId: String, itemId: VersionedId[ObjectId]) extends V2Error("Invalid Collection ID", "The idem did not have a valid collection identifier.", s"invalid collectionId in item $collectionId in item ${itemId}")

  case class orgCantAccessCollection(orgId: ObjectId, collectionId: String, accessType: String) extends V2Error("Organization Can't Access Collection", "The request is not valid because the associated organization does not have the requested access for the collection.", s"The org: $orgId can't access ($accessType) collection: $collectionId")

  case object default extends V2Error("Default", "?", "Failed to grant access")

  case class cantLoadSession(id: String) extends V2Error("Can't Load Session", "The provided session cannot be loaded.", s"Can't load session with id $id", NOT_FOUND)

  case class noItemIdInSession(id: String) extends V2Error("No Item ID in Session", "The session specified by the request did not correspond to an item id.", s"no item id in session: $id")

  case class cantParseItemId(id: String) extends V2Error("Can't Parse Item ID", "The item id provided by the request could not be parsed.", s"Can't parse itemId: $id")

  case class cantFindItemWithId(vid: VersionedId[ObjectId]) extends cantFindById("Can't Find Item with ID", "The item with the id provided by the request could not be found.", "item", vid.toString())

  case class cantFindOrgWithId(orgId: ObjectId) extends cantFindById("Can't Find Organization with ID", "The organization with the id provided by the request could not be found.", "org", orgId.toString)

  case class cantFindMetadataSetWithId(metadataSetId: ObjectId) extends cantFindById("Can't Find Metadata Set with ID", "The metadata set with the id provided by the request could not be found.", "metadataSet", metadataSetId.toString)

  case class cantFindAssessmentWithId(assessmentId: ObjectId) extends cantFindById("Can't Find Assessment with ID", "The assessment with the id provided by the request could not be found.", "assessment", assessmentId.toString)

  case class addAnswerRequiresId(assessmentId: ObjectId) extends V2Error("Adding an Answer Requires Participant ID", "The request specified to add an answer to the assessment, but an identifier for the participant was not specified.", s"Cannot add an answer to assessment $assessmentId without id")

  case class aggregateRequiresItemId(assessmentId: ObjectId) extends V2Error("Aggregate Requires Item Id", "The request specified to aggregate the data for an assessment, but item_id was not specified in the query string.", s"Aggregate for $assessmentId requires item_id in query string")

  case class cantFindAssessmentTemplateWithId(assessmentTemplateId: ObjectId) extends cantFindById("Can't Find Assessment Template with ID", "The assessment template with the id provided by the request could not be found.", "assessmentTemplate", assessmentTemplateId.toString)

  abstract class cantFindById(title: String, description: String, name: String, id: String) extends V2Error(title, description, s"Can't find $name with id $id", NOT_FOUND)

  case class invalidPval(pval: Long, collectionId: String, orgId: ObjectId) extends V2Error("Invalid Permission Value", "The permission value for the request was not valid for the collection and organization.", s"Unknown pval: $pval for collection $collectionId in org: $orgId")

  case class errorSaving(msg: String = "Error Saving") extends V2Error("Error Saving", "The server reported a non-specific error saving the resource.", msg)

  case class incorrectJsonFormat(badJson: JsValue, error: Option[JsError] = None) extends V2Error("Incorrect JSON format", "The format of the JSON specific by the request was incorrect for the request asked to be performed.", s"Bad json format ${Json.stringify(badJson)}") {

    implicit val pathWrites: Writes[JsPath] = new Writes[JsPath] {
      override def writes(o: JsPath): JsValue = Json.obj("path" -> o.toJsonString)
    }

    implicit val errorWrites: Writes[ValidationError] = new Writes[ValidationError] {
      override def writes(o: ValidationError): JsValue = Json.obj("message" -> o.message)
    }

    implicit val tupleWrites: Writes[(JsPath, Seq[ValidationError])] = new Writes[(JsPath, Seq[ValidationError])] {
      override def writes(o: (JsPath, Seq[ValidationError])): JsValue = Json.obj("path" -> o._1.toString, "errors" -> Json.toJson(o._2))
    }
    override def json = {

      val jsonErrors = error.map { e =>
        Json.obj(
          "json-errors" -> JsArray(
            e.errors.map(e => Json.toJson(e))))
      }.getOrElse(Json.obj())
      super.json ++ jsonErrors
    }
  }

  case class orgDoesntReferToCollection(orgId: ObjectId, collectionId: String)
    extends V2Error("Organization Doesn't Refer to Collection", "The orgianization provided by the request does not refer to the collection provided by the request.", s"org $orgId doesn't refer to collection: $collectionId", UNAUTHORIZED)

  case class inaccessibleItem(itemId: VersionedId[ObjectId], orgId: ObjectId, p: Permission)
    extends V2Error("Inaccessible Item", "The organization specified by the request cannot access the item with the specified permission.", s"org: $orgId can't access item: $itemId with permission ${p.name}", UNAUTHORIZED)

  case class unAuthorized(errors: String*) extends V2Error("Unauthorized", "The request was not authorized with the provided credentials.", errors.mkString(", "), UNAUTHORIZED)

  case class insufficientPermission(pval: Long, requestedPermission: Permission)
    extends V2Error("Insufficient Permission", "The provided authentication does not have the permissions necessary to process the request.", s"${Permission.toHumanReadable(pval)} does not allow ${requestedPermission.name}", UNAUTHORIZED)

  case class cantFindSession(id: String) extends V2Error("Can't Find Session", "The session with the id provided by the request could not be found.", s"Can't find session with id: $id", NOT_FOUND)

  case class sessionDoesNotContainResponses(sessionId: String) extends V2Error("Session Does Not Contain Responses", "The session specified by the request does not contain any responses.", s"session: $sessionId does not contain any responses")
}
