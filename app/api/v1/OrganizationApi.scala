package api.v1

import org.bson.types.ObjectId
import models.Organization
import play.api.libs.json.{JsValue, Json}
import controllers.auth.BaseApi
import api._
import play.api.Logger
import com.mongodb.casbah.Imports._
import scala.Left
import scala.Some
import scala.Right

/**
 * The Organization API
 */
object OrganizationApi extends BaseApi {

  val childPath = Organization.path + ".1"

  /**
   * Returns a list of organizations visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiActionRead { request =>
    doList(request.ctx.organization, q, f, c, sk, l)
  }

  def listWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiActionRead { request =>
    if(orgId == request.ctx.organization || Organization.isChild(request.ctx.organization,orgId))
      doList(orgId, q, f, c, sk, l)
    else
      Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, childrenOnly: Boolean = false) = {
    val key = if ( childrenOnly ) childPath else Organization.path
    val initSearch = MongoDBObject(key -> orgId)
    QueryHelper.list(q, f, c, sk,l, Organization, Some(initSearch))
  }

  /**
   * Returns a list of organizations visible to the organization in the request context
   *
   * @return
   */
  def getChildren(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiActionRead { request =>
    doList(request.ctx.organization, q, f, c, sk, l, childrenOnly =  true)
  }

  def getChildrenWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiActionRead { request =>
    if(orgId == request.ctx.organization || Organization.isChild(request.ctx.organization,orgId))
      doList(orgId, q, f, c, sk, l, childrenOnly = true)
    else
      Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
  }
  /**
   * Returns an Organization by its id
   *
   * @param id The organization id
   * @return
   */
  def getOrganization(id: ObjectId) = ApiActionRead { request =>
    Organization.findOneById(id) match {
        case Some(org) =>  {
          if (request.ctx.organization == org.id){
            Ok(Json.toJson(org))
          }else{
            Organization.getTree(request.ctx.organization).find(_.id == org.id) match {
              case Some(_) => Ok(Json.toJson(org))
              case None => Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
            }
          }
        }
        case None => NotFound(Json.toJson(ApiError.UnknownOrganization))
    }
  }

  /**
   * Returns the organization in the request context
   *
   * @return
   */
  def getDefaultOrganization = ApiActionRead { request =>
    Ok(Json.toJson(Organization.findOneById( request.ctx.organization )))
  }

  private def unknownOrganization = NotFound(Json.toJson(ApiError.UnknownOrganization))
  private def parseChildren(json: JsValue, elseValue: Seq[ObjectId]):Seq[ObjectId] = {
    (json \ "children").asOpt[Seq[String]].map( seq => seq.map( new ObjectId(_)) ).getOrElse(elseValue)
  }

  /**
   * Updates an organization
   *
   * @return
   */
  def updateOrganization(id: ObjectId) = ApiAction { request =>
    Organization.findOneById(id).map( original =>
    {
      request.body.asJson match {
        case Some(json) => {
          val name = (json \ "name").asOpt[String].getOrElse(original.name)
          Organization.updateOrganization(Organization(name = name,id = id)) match {
            case Right(o) => Ok(Json.toJson(o))
            case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateOrganization(e.clientOutput)))
          }
        }
        case _ => jsonExpected
      }
    }).getOrElse(unknownOrganization)
  }

  /**
   * Deletes an organization
   */
  def deleteOrganization(id: ObjectId) = ApiAction { request =>
    if ( id == request.ctx.organization) {
      Forbidden(Json.toJson(ApiError.CantDeleteMainOrg))
    } else {
      Organization.findOneById(id) match {
        case Some(toDelete) => {
          Organization.delete(id) match {
            case Right(_) => Ok(Json.toJson(toDelete))
            case Left(e) => InternalServerError(Json.toJson(ApiError.RemoveOrganization(e.clientOutput)))
          }
        }
        case _ => unknownOrganization
      }
    }
  }

}
