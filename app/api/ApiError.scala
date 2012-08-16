package api

import play.api.libs.json._
import controllers.{LogType, Log}


/**
 * A class representing errors returned by the APIs
 */
case class ApiError(code: Int, message: String, moreInfo:Option[String] = None){
  def format(s: String):ApiError = {
    copy(message = this.message.format(s))
  }
  def apply(otherInfo:Option[String]):ApiError = {
    copy(moreInfo = otherInfo)
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
  val InvalidField        = ApiError(203, "Field %s is invalid")

  // Organization API
  val IdsDoNotMatch       = ApiError(300, "Specified id does not match the one in the json")
  val CantDeleteMainOrg   = ApiError(301, "You cannot delete your main organization")
  val OrgNameMissing      = ApiError(303, "You need to specify a name for the organization")
  val InsertOrganization  = ApiError(304, "Failed to insert organization")
  val UpdateOrganization  = ApiError(305, "Failed to update organization")
  val RemoveOrganization  = ApiError(306, "Failed to remove organization")
  val UnauthorizedOrganization  = ApiError(307, "You do not have access to the given organization")

  // Collections aPI
  val CollectionNameMissing = ApiError(400, "You need to specify a name for the collection")
  val UnknownCollection     = ApiError(401, "Unknown collection")
  val InsertCollection      = ApiError(402, "Failed to insert collection")
  val UpdateCollection      = ApiError(403, "Failed to update collection")
  val AddToOrganization     = ApiError(404, "Failed to link collection to organization(s)")

  // User API
  val UserRequiredFields    = ApiError(500, "username, fullname and email are required")
  val UnknownUser           = ApiError(501, "Unknown user")
  val UsersInOrganization   = ApiError(502, "An error occurred when trying to find users in your organization")
  val CreateUser            = ApiError(503, "Could not create user")
  val UpdateUser            = ApiError(504, "Could not update user")
  val DeleteUser            = ApiError(505, "An error occurred when deleting the user")

  // Item Session API
  val ItemSessionRequiredFields = ApiError(500, "start property must be provided")
  val ItemIdRequired = ApiError(500, "no item id was provided for the session")



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
