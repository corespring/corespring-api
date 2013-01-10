package developer.controllers

import play.api.mvc._
import controllers.{Log, Assets}
import securesocial.core._
import play.api.libs.json._
import api.ApiError
import org.bson.types.ObjectId
import models.{User, Organization}
import controllers.auth.{OAuthConstants, OAuthProvider, Permission}
import scala.Left
import play.api.libs.json.JsObject
import play.api.libs.json.JsBoolean
import scala.Some
import scala.Right
import scala.Left
import securesocial.core.SocialUser
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import scala.Right
import securesocial.core.PasswordInfo
import play.api.libs.json.JsObject
import scala.Left
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import scala.Right
import play.api.mvc.SimpleResult
import play.api.libs.json.JsObject

object Developer extends Controller with SecureSocial{

  def at(path:String,file:String) = Assets.at(path,file)

  def home = Action{implicit request =>
    request.session.get(SecureSocial.UserKey) match {
      case Some(username) =>
        User.getUser(username) match {
        case Some(user) => if(!user.hasRegisteredOrg){
          Redirect("/developer/org/form")
        }else{
          Assets.at("/public/developer", "index.html")(request)
        }
        case None => Assets.at("/public/developer", "index.html")(request)
      }
      case None => Assets.at("/public/developer", "index.html")(request)
    }
  }

  private def addAccessToken(result:SimpleResult[AnyContent],user:User)(implicit request:RequestHeader):Result = {
    OAuthProvider.register(user.orgs(0).orgId) match {
      case Right(apiClient) => OAuthProvider.getAccessToken(OAuthConstants.ClientCredentials,
        apiClient.clientId.toString,
        apiClient.clientSecret,Some(user.userName)) match {
        case Right(accessToken) => result.withHeaders(request.headers + "access_token" -> accessToken.tokenId)
        case Left(error) => {
          Log.e(error.message)
          result
        }
      }
      case Left(error) => {
        Log.e(error.message)
        result
      }
    }
  }

  def login = Action{ request =>
    Redirect("/login").withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }
  def isLoggedIn = Action { request =>
    val username = request.session.get(SecureSocial.UserKey)
    if(username.isDefined){
      User.getUser(username.get) match {
        case Some(user) => {
          if (user.provider == "userpass"){
            Ok(JsObject(Seq("isLoggedIn" -> JsBoolean(true), "username" -> JsString(user.userName))))
          }else{
            Ok(JsObject(Seq("isLoggedIn" -> JsBoolean(true), "username" -> JsString(user.fullName.split(" ")(0)))))
          }
        }
        case None => Ok(JsObject(Seq("isLoggedIn" -> JsBoolean(false))))    //this can occur if the cookies are still set but the user has been deleted
      }
    }else{
      Ok(JsObject(Seq("isLoggedIn" -> JsBoolean(false))))
    }
  }
  def register = Action { request =>
    Redirect("/signup").withSession(request.session + ("securesocial.originalUrl" -> "/developer/home"));
  }
  def logout = Action {implicit request =>
    Redirect("/developer/home").withSession(session - SecureSocial.UserKey - SecureSocial.ProviderKey)
  }

  def getOrganization = SecuredAction(){ request =>
    User.getUser(request.user.id) match {
      case Some(user) => {
        val orgs = User.getOrganizations(user,Permission.Read)
        //get the first organization besides the public corespring organization. for now, we assume that the person is only registered to one private organization
        orgs.find(o => o.id.toString != Organization.CORESPRING_ORGANIZATION_ID) match {
          case Some(o) => Ok(Json.toJson(o))
          case None => NotFound(Json.toJson(ApiError.MissingOrganization))
        }
      }
      case None => InternalServerError("could not find user...after authentication. something is very wrong")
    }
  }
  def createOrganizationForm = SecuredAction(){ request =>
    Ok(developer.views.html.org_new(request.user))
  }
  //TODO requires two phase commit, one part updating the users and the other updating organizations
  def createOrganization = SecuredAction(){ request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val name = (json \ "name").asOpt[String]
            if ( name.isEmpty ) {
              BadRequest( Json.toJson(ApiError.OrgNameMissing))
            } else {
              val optParent:Option[ObjectId] = (json \ "parent_id").asOpt[String].map(new ObjectId(_))
              val organization = Organization(name.get)
              Organization.insert(organization,optParent) match {
                case Right(org) => {
                  User.getUser(request.user.id) match {
                    case Some(user) => {
                      User.removeOrganization(user.id,new ObjectId(Organization.CORESPRING_ORGANIZATION_ID)) match {
                        case Right(_) => User.addOrganization(user.id,org.id,Permission.Write) match {
                          case Right(_) => Ok(Json.toJson(org))
                          case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateUser(error.clientOutput)))
                        }
                        case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateUser(error.clientOutput)))
                      }
                    }
                    case None => InternalServerError("an error that should never happen happened")
                  }
                }
                case Left(e) => InternalServerError(Json.toJson(ApiError.InsertOrganization(e.clientOutput)))
              }
            }
          }
        }
      }
      case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
    }
  }

  def getOrganizationCredentials(orgId: ObjectId) = SecuredAction(){ request =>
    User.getUser(request.user.id) match {
      case Some(user) => {
        if(user.orgs.exists(uo => uo.orgId == orgId)){
          Organization.findOneById(orgId) match {
            case Some(org) => {
              OAuthProvider.register(org.id) match {
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

