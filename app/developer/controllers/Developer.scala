package developer.controllers

import api.ApiError
import common.config.AppConfig
import controllers.Assets
import controllers.auth.{BaseApi, OAuthProvider, Permission}
import models.{User, Organization}
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.mvc._
import scala._
import models.auth.ApiClient
import scala.Left
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import scala.Right
import play.api.libs.json.JsObject
import common.log.PackageLogging
import securesocial.core.{SecureSocial, Authenticator}
import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}
import developer.controllers.routes.{Developer => DeveloperRoutes}

/**
 * TODO: remove magic strings
 */
object Developer extends Controller with BaseApi with PackageLogging{

  def at(path:String,file:String) = Assets.at(path,file)

  private def userFromRequest(r:Request[AnyContent]) : Option[User] = {
    val result : Validation[String,User] = for{
      authenticator <- SecureSocial.authenticatorFromRequest(r).toSuccess("Can't find authenticator")
      user <- User.getUser(authenticator.userId).toSuccess(s"Can't find user with id: $authenticator.userId")
    } yield user

    result match {
      case Success(u) => Some(u)
      case Failure(e) => {
        Logger.warn( s"[userFromRequest] - $e")
        None
      }
    }
  }

  def home = Action{implicit request =>

    val defaultView = at("/public/developer", "index.html")(request)

    userFromRequest(request).map{ u =>
      if(u.hasRegisteredOrg) defaultView else Redirect(DeveloperRoutes.createOrganizationForm().url)
    }.getOrElse(defaultView)
  }

  def login = Action{ request =>
    Redirect(securesocial.controllers.routes.LoginPage.login().url).withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }

  def isLoggedIn = Action { request =>

    def json(isLoggedIn:Boolean, username:Option[String] = None) = JsObject(Seq("isLoggedIn" -> JsBoolean(isLoggedIn)) ++ username.map( "username" -> JsString(_)))

    userFromRequest(request).map{ u =>
      val username = Some(if(u.provider == "userpass") u.userName else u.fullName.split(" ")(0))
      Ok(json(true, username))
    }.getOrElse(
     Ok(json(false) )
    )
  }

  def register = Action { request =>
    Redirect("/signup").withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }

  def logout = Action {implicit request =>
    val result = securesocial.controllers.LoginPage.logout(request)
    Redirect(DeveloperRoutes.home().url)
  }

  def getOrganization = SecuredAction{ request =>
    User.getUser(request.user.id) match {
      case Some(user) => {
        val org:Option[Organization] = User.getOrg(user,Permission.Read)
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

  def createOrganizationForm = SecuredAction{ request =>
    Ok(developer.views.html.org_new(request.user))
  }

  def createOrganization = SecuredAction(false){ request =>
    import scalaz._
    import Scalaz._

    def makeOrg(json :JsValue) : Option[Organization] =  (json \ "name").asOpt[String].map{ n =>
      import common.models.json._
      Organization(n, (json \ "parent_id").asOpt[ObjectId].toList)
    }

    def makeApiClient(orgId:ObjectId) : Option[ApiClient] = OAuthProvider.createApiClient(orgId) match {
      case Left(e) => None
      case Right(c) => Some(c)
    }

    def setOrg(userId : ObjectId, orgId : ObjectId) : Option[ObjectId] = {
      User.setOrganization(userId,orgId, Permission.Write) match {
        case Left(e) => None
        case _ => Some(userId)
      }
    }

    val validation : Validation[String,(Organization,ApiClient)]= for{
      user <- User.getUser(request.user.id).toSuccess("Unknown user")
      okUser <- if(user.hasRegisteredOrg) Failure("Org already registered") else Success(user)
      json <- request.body.asJson.toSuccess("Json expected")
      orgToCreate <- makeOrg(json).toSuccess("Couldn't create org")
      orgId <- Organization.insert(orgToCreate).toSuccess("Couldn't create org")
      updatedUserId <- setOrg(okUser.id,orgId).toSuccess("Couldn't set org")
      apiClient <- makeApiClient(orgId).toSuccess("Couldn't create api client")
    } yield (orgToCreate,apiClient)

    validation match {
      case Failure(s) => BadRequest(s)
      case Success((o,c)) => Ok(developer.views.html.org_credentials(c.clientId.toString, c.clientSecret, o.name))
    }
  }

  def getOrganizationCredentials(orgId: ObjectId) = SecuredAction{ request =>
    User.getUser(request.user.id) match {
      case Some(user) => {
        if(user.org.orgId == orgId){
          Organization.findOneById(orgId) match {
            case Some(org) => {
              OAuthProvider.createApiClient(org.id) match {
                case Right(client) => Ok(developer.views.html.org_credentials(client.clientId.toString,client.clientSecret,org.name))
                case Left(error) => BadRequest(Json.toJson(error))
              }
            }
            case None => InternalServerError("could not find organization, after authentication. this should never occur")
          }
        }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
      }
      case None => InternalServerError("could not find user...after authentication. something is very wrong")
    }
  }

  def handleStartSignUp = Action { implicit request =>
    val action = securesocial.controllers.Registration.handleStartSignUp(request)
    action match {
      case BadRequest => action
      case _ => Ok(developer.views.html.registerDone())
    }
  }

  def handleSignUp(token:String) = Action { request =>
    MyRegistration.handleSignUp(token)(request)
  }
}

