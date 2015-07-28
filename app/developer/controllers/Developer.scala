package developer.controllers

import controllers.Assets
import developer.ServiceLookup
import developer.controllers.routes.{ Developer => DeveloperRoutes }
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.models.json.ObjectIdFormat
import org.corespring.web.api.v1.errors.ApiError
import org.corespring.models.auth.{ ApiClient, Permission }
import org.corespring.models.{ ContentCollection, Organization, User }
import play.api.libs.json._
import play.api.mvc._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }
import securesocial.core.{ SecuredRequest, IdentityId, SecureSocial }
import scala.concurrent.{ ExecutionContext, Future }

/**
 * TODO: remove magic strings
 */
object Developer extends Controller with SecureSocial with PackageLogging {

  import ExecutionContext.Implicits.global

  implicit val writeOid = ObjectIdFormat
  implicit val writeOrg = ServiceLookup.jsonFormatting.writeOrg

  def at(path: String, file: String) = Assets.at(path, file)

  private def getUser(id: IdentityId): Option[User] = ServiceLookup.userService.getUser(id.userId, id.providerId)

  private def userFromRequest(r: Request[AnyContent]): Option[User] = {
    val result: Validation[String, User] = for {
      authenticator <- SecureSocial.authenticatorFromRequest(r).toSuccess("Can't find authenticator")
      user <- getUser(authenticator.identityId).toSuccess(s"Can't find user with id: $authenticator.userId")
    } yield user

    result match {
      case Success(u) => Some(u)
      case Failure(e) => {
        logger.warn(s"[userFromRequest] - $e")
        None
      }
    }
  }

  private def hasRegisteredOrg(u: User) = {
    u.org.orgId != AppConfig.demoOrgId
  }

  def home = Action.async {
    implicit request =>

      val defaultView: Future[SimpleResult] = at("/public/developer", "index.html")(request)

      userFromRequest(request)
        .filterNot(hasRegisteredOrg)
        .map(u => Future(Redirect(DeveloperRoutes.createOrganizationForm().url)))
        .getOrElse(defaultView)
  }

  def login = Action {
    request =>
      Redirect(securesocial.controllers.routes.LoginPage.login().url).withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }

  def isLoggedIn = SecuredAction(ajaxCall = true) {
    request: SecuredRequest[AnyContent] =>

      def json(isLoggedIn: Boolean, username: Option[String] = None) = JsObject(Seq("isLoggedIn" -> JsBoolean(isLoggedIn)) ++ username.map("username" -> JsString(_)))
      val userId: IdentityId = request.user.identityId
      getUser(userId) match {
        case Some(user) => {
          val username = if (user.provider == "userpass") user.userName else user.fullName.split(" ").head
          Ok(json(true, Some(username)))
        }
        case None => {
          // this can occur if the cookies are still set but the user has been deleted
          Ok(json(false))
        }
      }
  }

  def register = Action {
    request =>
      Redirect("/signup").withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }

  def logout = Action {
    implicit request =>
      val result = securesocial.controllers.LoginPage.logout(request)
      Redirect(DeveloperRoutes.home().url)
  }

  def getOrganization = SecuredAction {
    request =>
      getUser(request.user.identityId) match {
        case Some(user) => {
          val org: Option[Organization] = ServiceLookup.userService.getOrg(user, Permission.Read)
          //get the first organization besides the public corespring organization. for now, we assume that the person is only registered to one private organization
          //TODO: this doesn't look right - need to discuss a fix for it.
          org.find(o => o.id != AppConfig.demoOrgId) match {
            case Some(o) => Ok(Json.toJson(o))
            case None => NotFound(Json.toJson(ApiError.MissingOrganization))
          }
        }
        case None => InternalServerError("could not find user...after authentication. something is very wrong")
      }
  }

  def createOrganizationForm = SecuredAction {
    request =>
      Ok(developer.views.html.org_new(request.user))
  }

  def createOrganization = SecuredAction(false) {
    request: SecuredRequest[AnyContent] =>

      def makeOrg(json: JsValue): Option[Organization] = (json \ "name").asOpt[String].map {
        n =>
          Organization(n, (json \ "parent_id").asOpt[ObjectId].toList)
      }

      def makeApiClient(orgId: ObjectId): Option[ApiClient] = ServiceLookup.apiClientService.createForOrg(orgId) match {
        case Left(e) => None
        case Right(c) => Some(c)
      }

      def setOrg(userId: ObjectId, orgId: ObjectId): Option[ObjectId] = {
        ServiceLookup.userService.setOrganization(userId, orgId, Permission.Write) match {
          case Left(e) => None
          case _ => Some(userId)
        }
      }

      def createDefaultCollection(orgId: ObjectId) =
        ServiceLookup.contentCollectionService.insertCollection(orgId,
          ContentCollection(ContentCollection.Default, orgId), Permission.Write)

      val validation: Validation[String, (Organization, ApiClient)] = for {
        user <- getUser(request.user.identityId).toSuccess("Unknown user")
        okUser <- if (hasRegisteredOrg(user)) Failure("Org already registered") else Success(user)
        json <- request.body.asJson.toSuccess("Json expected")
        orgToCreate <- makeOrg(json).toSuccess("Couldn't create org")
        org <- ServiceLookup.orgService.insert(orgToCreate, None).right.toOption.toSuccess("Couldn't create org")
        updatedIdentityId <- setOrg(okUser.id, org.id).toSuccess("Couldn't set org")
        // Ensure default collection is created for this organisation
        defaultCollection <- createDefaultCollection(org.id).right.toOption.toSuccess("Couldn't create default collection")
        apiClient <- makeApiClient(org.id).toSuccess("Couldn't create api client")
      } yield (org, apiClient)

      validation match {
        case Failure(s) => BadRequest(s)
        case Success((o, c)) => Ok(developer.views.html.org_credentials(c.clientId.toString, c.clientSecret, o.name))
      }
  }
  /*
   def getOrganizationCredentials(orgId: ObjectId) = SecuredAction {
    request =>
      User.getUser(request.user.identityId) match {
        case Some(user) => {
          if (user.org.orgId == orgId) {
            Organization.findOneById(orgId) match {
              case Some(org) => {
                OAuthProvider.createApiClient(org.id) match {
                  case Right(client) => Ok(developer.views.html.org_credentials(client.clientId.toString, client.clientSecret, org.name))
                  case Left(error) => BadRequest(Json.toJson(error))
                }
              }
              case None => InternalServerError("could not find organization, after authentication. this should never occur")
            }
          } else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
        }
        case None => InternalServerError("could not find user...after authentication. something is very wrong")
      }
  }
   */
  def getOrganizationCredentials(orgId: ObjectId) = SecuredAction {
    request =>
      getUser(request.user.identityId) match {
        case Some(user) => {
          if (user.org.orgId == orgId) {
            ServiceLookup.orgService.findOneById(orgId) match {
              case Some(org) => {
                ServiceLookup.apiClientService.createForOrg(org.id) match {
                  case Right(client) => Ok(developer.views.html.org_credentials(client.clientId.toString, client.clientSecret, org.name))
                  case Left(error) => BadRequest(Json.toJson(error))
                }
              }
              case None => InternalServerError("could not find organization, after authentication. this should never occur")
            }
          } else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
        }
        case None => InternalServerError("could not find user...after authentication. something is very wrong")
      }
  }

  def handleStartSignUp = Action.async {
    implicit request =>
      val action = securesocial.controllers.Registration.handleStartSignUp(request)
      action.transform({ r =>
        r match {
          case BadRequest => action
          case _ => Ok(developer.views.html.registerDone())
        }
      }, e => e)
      action
  }

  def handleSignUp(token: String) = Action.async { request => MyRegistration.handleSignUp(token)(request) }
}

