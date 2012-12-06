package developer.controllers

import play.api.mvc.Controller
import controllers.Assets
import securesocial.core.SecureSocial
import play.api.libs.json.Json
import api.ApiError
import org.bson.types.ObjectId
import models.{User, Organization}
import controllers.auth.Permission

object Developer extends Controller with SecureSocial{

  def at(path:String,file:String) = Assets.at(path,file)


  def organizations = SecuredAction(){ request =>
    Ok(developer.views.html.organizations(request.user))
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
                      User.addOrganization(user.id,org.id,Permission.All) match {
                        case Right(_) => Ok(Json.toJson(org))
                        case Left(error) => InternalServerError(Json.toJson(ApiError.UpdateUser(error.clientOutput)))
                      }
                    }
                    case None => InternalServerError("an error that should never happen happened")
                  }
                  Ok(Json.toJson(org))
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

  def getOrganizations = SecuredAction(){ request =>
    User.getUser(request.user.id) match {
      case Some(user) => Ok(Json.toJson(User.getOrganizations(user,Permission.All)))
      case None => InternalServerError("could not find user...after authentication. something is very wrong")
    }
  }

  def getOrganizationCredentials(orgId: ObjectId) = SecuredAction(){ request =>
    User.getUser(request.user.id) match {
      case Some(user) => {
        if(user.orgs.exists(uo => uo.orgId == orgId)){
          Ok
        }else Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
      }
      case None => InternalServerError("could not find user...after authentication. something is very wrong")
    }
  }
}

