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

  override def toString = "%d: %s [%s]".format(code,message, moreInfo)
}


object ApiError {

  trait BaseError {
    def index : Int

    private var errors : List[ApiError] = List()

    def make(msg:String) : ApiError = {
      val e = ApiError(code, msg)
      errors = e :: errors
      e
    }

    def code = (index.toString + "%02d" format errors.length).toInt
    def allErrors : String = errors.reverse.mkString("\n")
  }

  trait ResourceError extends BaseError{
    def name : String
    def template : String = "error occurred when attempting to %s %s"
    val NotFound = make("%s not found".format(name))
    val Delete = make(template.format("delete",name))
    val Update = make(template.format("update", name))
    val Create = make(template.format("create",name))
  }

  val Item = new ResourceError{
    def index = 1
    def name = "item"
    val CollectionIsRequired = make("A collection id for the %s is required".format(name))
    val Clone = make("Error cloning")
  }

  // OAuth Provider
  val InvalidCredentials  = ApiError(100, "Invalid credentials")
  val UnsupportedFlow     = ApiError(101, "Unsupported OAuth flow")
  val InvalidToken        = ApiError(102, "The Access Token is invalid or has expired")
  val InvalidTokenType    = ApiError(103, "Invalid Token type. Only Bearer tokens are supported")
  val MissingCredentials  = ApiError(104, "Missing credentials in request")
  val UnknownOrganization = ApiError(105, "Unknown organization")
  val MissingOrganization = ApiError(106, "Organization not specified")
  val OperationError      = ApiError(107, "There was an error processing your request")
  val ExpiredToken        = ApiError(108, "Your access token expired on %s. Please request a new one")

  // Base Api
  val UserIsRequired      = ApiError(150, "User is required")
  val JsonExpected        = ApiError(151, "You request does not contain a valid json")

  // Common DAOs
  val CantSave            = ApiError(200, "There was an error saving your information")
  val IdNotNeeded         = ApiError(201, "An id cannot be specified for this operation")
  val InvalidQuery        = ApiError(202, "Your query is invalid")
  val UnknownFieldOrOperator = ApiError(203, "Unknown field or operator: %s")
  val InvalidField        = ApiError(204, "Field %s is invalid")
  val CollIdNotNeeded      = ApiError(205, "a collection id cannot be specified for this operation")
  val StringToMongo         = ApiError(206, "could not parse string into db object")

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
  val CollectionUnauthorized = ApiError(405, "You do not have permissions for the specified collection")

  // User API
  val UserRequiredFields    = ApiError(500, "userName, fullName and email are required")
  val UnknownUser           = ApiError(501, "Unknown user")
  val UsersInOrganization   = ApiError(502, "An error occurred when trying to find users in your organization")
  val CreateUser            = ApiError(503, "Could not create user")
  val UpdateUser            = ApiError(504, "Could not update user")
  val DeleteUser            = ApiError(505, "An error occurred when deleting the user")

  // Item Session API
  val ItemSessionRequiredFields = ApiError(600, "start property must be provided")
  val ItemIdRequired = ApiError(601, "no item id was provided for the session")
  val UnauthorizedItemSession = ApiError(602, "you are not authorized to access the given item session")
  val CreateItemSession = ApiError(603, "could not create item session")
  val UpdateItemSession = ApiError(604, "could not update item session")
  val ItemSessionNotFound = ApiError(605, "item session specified could not be found")
  val ItemSessionFinished = ApiError(606, "item session is alredy finished - can't update it")


  //amazon s3
  val AmazonS3Client          = ApiError(800, "an exception occured on the when communicating with S3")
  val AmazonS3Server          = ApiError(801, "S3 was unable to service the request")
  val ContentLength           = ApiError(802, "Error regarding content length header")
  val S3NotIntialized         = ApiError(803, "S3 service not initialized")
  val S3Write                 = ApiError(804, "error writing data to S3")

  //Resource Api
  val FilenameTaken           = ApiError(900, "Filename is taken - please choose another name")
  val ResourceNotFound        = ApiError(901, "Can't find resource")
  val ResourceNameTaken       = ApiError(902, "Resource name is taken - please choose another")
  val FilenameIsRequired      = ApiError(903, "Filename is required")

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
