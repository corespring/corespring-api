package basiclti

import play.api.mvc.{Request, AnyContent}
import web.controllers.utils.ConfigLoader

/**
 * Basic LTI Launch Data
 */
case class LisPerson(
                      givenName: Option[String], // all lisXXXX parameters are recommended
                      familyName: Option[String],
                      fullName: Option[String],
                      contactEmailPrimary: Option[String]
                      )

case class LaunchPresentation(
                               locale: String, // the spec does not say if this is required/optional or recommended ...
                               documentTarget: Option[String], // recommended
                               cssUrl: Option[String], // // the spec does not say if this is required/optional or recommended BUT it's optional to me
                               width: Option[Int], // recommended
                               height: Option[Int], // recommended
                               returnUrl: Option[String] // recommended
                             )

case class ToolConsumer(
                         productFamilyCode: Option[String], // recommended
                         infoVersion: Option[String], // recommended
                         instanceGuid: Option[String], // strongly recommended in multi-tenacy systems
                         instanceName: Option[String], // recommended
                         instanceDescription: Option[String], // optional
                         instanceUrl: Option[String], // optional
                         instanceContactEmail: Option[String] // recommended
                         )
case class LaunchData(
    messageType: String,
    messageVersion: String,
    resourceLinkId: String,
    resourceLinkTitle: Option[String], // recommended
    resourceLinkDescription: Option[String], // optional
    userId: Option[String], // recommended
    userImage: Option[String], // optional
    roles: Option[String], // recommended.  a comma separated list of URN values for roles
    lisPerson: LisPerson,
    roleScopeMentor: Option[String], // optional. a comma separated list of user id values
    contextId: Option[String], // recommended
    contextType:  Option[String], // optional.  a comma separated list of URN values
    contextTitle: Option[String], // recommended
    contextLabel: Option[String], // recommended
    launchPresentation: LaunchPresentation,
    toolConsumer: ToolConsumer,
    corespringItemId: Option[String]
    // recommended
    //
    // not including custom fields for now
    //

)

object LaunchData {
  lazy val BaseUrl = ConfigLoader.get("BASE_URL").getOrElse("http://localhost:9000");

  val LtiMessageType = "lti_message_type"
  val BasicLtiLaunchRequest = "basic-lti-launch-request"
  val LtiVersion = "lti_version"
  val LtiVersion1 = "LtiLTI-1p0"
  val ResourceLinkId = "resource_link_id"
  val ResourceLinkTitle = "resource_link_title"
  val ResourceLinkDescription = "resource_link_description"
  val UserId = "user_id"
  val UserImage = "user_image"
  val Roles = "roles"

  val LisPersonGivenName = "lisPersonGivenName"
  val LisPersonFamilyName = "lisPersonFamilyName"
  val LisPersonFullName = "lisPersonFullName"
  val LisPersonContactEmailPrimary = "lisPersonContactEmailPrimary"
  val RoleScopeMentor = "roleScopeMentor"

  val ContextId = "context_id"
  val ContextType = "context_type"
  val ContextTitle = "context_title"
  val ContextLabel = "context_label"
  val LaunchPresentationLocale = "launch_presentation_locale"
  val LaunchPresentationDocumentTarget = "launch_presentation_document_target"
  val Iframe = "iframe"
  val Frame = "frame"
  val Window = "window"
  val LaunchPresentationCssUrl = "launch_presentation_css_url"
  val LaunchPresentationWidth = "launch_presentation_width"
  val LaunchPresentationHeight = "launch_presentation_height"
  val LaunchPresentationReturnUrl = "launch_presentation_return_url"
  val ToolConsumerInfoProductFamilyCode = "tool_consumer_info_product_family_code"
  val ToolConsumerInfoVersion = "tool_consumer_info_version"
  val ToolConsumerInstanceGuid = "tool_consumer_instance_guid"
  val ToolConsumerInstanceName = "tool_consumer_instance_name"
  val ToolConsumerInstanceDescription = "tool_consumer_instance_description"
  val ToolConsumerInstanceUrl = "tool_consumer_instance_url"
  val ToolConsumerInstanceContactEmail = "tool_consumer_instance_contact_email"
  val CoreSpringItemId = "custom_corespring_item_id"

  val requiredFields = List(LtiMessageType, LtiVersion, ResourceLinkId, LaunchPresentationLocale, CoreSpringItemId)

  private def checkRequired(data: Map[String, String], required: List[String] = requiredFields): Option[List[String]] = {
    def check(fields: List[String], errors: List[String]): List[String] = {
      if ( fields.isEmpty ) errors else {
        val e = if ( data.isDefinedAt(fields.head) ) errors else "Missing field form: %s".format(fields.head) :: errors
        check(fields.tail, e)
      }
    }
    check(required, List()) match {
      case errors: List[_] if errors.size > 0 => Some(errors)
      case _ => None
    }
  }

  def buildFromRequest(request: Request[AnyContent], required: List[String] = requiredFields): Either[List[String], LaunchData] = {
    request.body.asFormUrlEncoded match {
      case Some(formData) => {
        val data = formData.mapValues(_.headOption.getOrElse(""))
        val errors = checkRequired(data,required)
        if ( errors.isDefined ) {
          Left(errors.get)
        } else {
          //todo : check width and height are Ints
          Right(LaunchData(
            data.get(LtiMessageType).get,
            data.get(LtiVersion).get,
            data.get(ResourceLinkId).get,
            data.get(ResourceLinkTitle),
            data.get(ResourceLinkDescription),
            data.get(UserId),
            data.get(UserImage),
            data.get(Roles),
            LisPerson(
              data.get(LisPersonGivenName),
              data.get(LisPersonFamilyName),
              data.get(LisPersonFullName),
              data.get(LisPersonContactEmailPrimary)
            ),
            data.get(RoleScopeMentor),
            data.get(ContextId),
            data.get(ContextType),
            data.get(ContextTitle),
            data.get(ContextLabel),
            LaunchPresentation(
              data.get(LaunchPresentationLocale).get,
              data.get(LaunchPresentationDocumentTarget),
              data.get(LaunchPresentationCssUrl),
              data.get(LaunchPresentationWidth).map(_.toInt),
              data.get(LaunchPresentationWidth).map(_.toInt),
              data.get(LaunchPresentationReturnUrl)
            ),
            ToolConsumer(
              data.get(ToolConsumerInfoProductFamilyCode),
              data.get(ToolConsumerInfoVersion),
              data.get(ToolConsumerInstanceGuid),
              data.get(ToolConsumerInstanceName),
              data.get(ToolConsumerInstanceDescription),
              data.get(ToolConsumerInstanceUrl),
              data.get(ToolConsumerInstanceContactEmail)
            ),
            data.get(CoreSpringItemId)
          ))
        }
      }
      case _ => Left(List("No BasicLTI form present in the request"))
    }
  }
}
