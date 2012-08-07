package api

import play.api.libs.json._


/**
 * A class representing errors returned by the APIs
 */

case class ApiError(code: Int, message: String, moreInfo: Option[String] = None) {
  def format(s: String):ApiError = {
    copy(message = this.message.format(s))
  }
}

object ApiError {
  // OAuth Provider
  val InvalidCredentials  = ApiError(100, "Invalid credentials")
  val UnsupportedFlow     = ApiError(101, "Unsupported OAuth flow")
  val InvalidToken        = ApiError(102, "The Access Token is invalid or has expired")
  val InvalidTokenType    = ApiError(103, "Invalid Token type. Only Bearer tokens are supported")
  val MissingCredentials  = ApiError(104, "Missing credentials in request")
  val UnknownOrganization = ApiError(105, "Unknown organization")
  val MissingOrganization = ApiError(106, "Organization not specified")
  val OperationError      = new ApiError(107, "There was an error processing your request")

  // Base Api
  val UserIsRequired      = new ApiError(150, "User is required")
  val JsonExpected        = new ApiError(151, "You request does not contain a valid json")

  // Common DAOs
  val CantSave            = ApiError(200, "There was an error saving your information")
  val IdNotNeeded         = ApiError(201, "An id cannot be specified for this operation")
  val InvalidQuery        = ApiError(202, "Your query is invalid")
  val UnknownFieldOrOperator = ApiError(203, "Unknown field or operator: %s")

  // Organization API
  val IdsDoNotMatch       = ApiError(300, "Specified id does not match the one in the json")
  val CantDeleteMainOrg   = ApiError(301, "You cannot delete your main organization")
  val OrgNameMissing      = ApiError(303, "You need to specify a name for the organization")

  // Collections aPI
  val CollectionNameMissing = ApiError(400, "You need to specify a name for the collection")
  val UnknownCollection     = ApiError(401, "Unknown collection")

  // User API
  val UserRequiredFields    = ApiError(500, "username, fullname and email are required")
  val UnknownUser           = ApiError(501, "Unknown user")


  implicit object ApiErrorWrites extends Writes[ApiError] {
    def writes(o: ApiError): JsValue = {
      JsObject(List(
        "code" -> JsNumber(o.code),
        "message" -> JsString(o.message),
        "moreInfo" -> JsString( o.moreInfo.getOrElse(""))
      ))
    }
  }
}
