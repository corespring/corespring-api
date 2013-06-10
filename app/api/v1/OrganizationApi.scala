package api.v1

import org.bson.types.ObjectId
import models.{User, Organization}
import play.api.libs.json._
import controllers.auth.BaseApi
import api._
import com.mongodb.casbah.Imports._
import play.api.mvc.Result
import controllers.Utils
import com.mongodb.casbah.commons.MongoDBObject
import scala.Left
import models.search.SearchCancelled
import play.api.libs.json.JsArray
import scala.Some
import scala.Right
import com.novus.salat.dao.SalatMongoCursor
import play.api.libs.json.JsObject
import common.config.AppConfig

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
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort:Option[String]) = ApiActionRead { request =>
    doList(request.ctx.organization, q, f, c, sk, l,sort)
  }

  def listWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort:Option[String]) = ApiActionRead { request =>
    if(orgId == request.ctx.organization || Organization.isChild(request.ctx.organization,orgId))
      doList(orgId, q, f, c, sk, l,sort)
    else
      Unauthorized(Json.toJson(ApiError.UnauthorizedOrganization))
  }

  private def doList(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, optsort:Option[String], childrenOnly: Boolean = false) = {
    val key = if ( childrenOnly ) childPath else Organization.path
    val initSearch = MongoDBObject(key -> orgId)
    def applySort(orgs:SalatMongoCursor[Organization]):Result = {
      optsort.map(Organization.toSortObj(_)) match {
        case Some(Right(sort)) => Ok(Json.toJson(Utils.toSeq(orgs.sort(sort).skip(sk).limit(l))))
        case None => Ok(Json.toJson(Utils.toSeq(orgs.skip(sk).limit(l))))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidSort(error.clientOutput)))
      }
    }
    q.map(Organization.toSearchObj(_,Some(initSearch))).getOrElse[Either[SearchCancelled,MongoDBObject]](Right(initSearch)) match {
      case Right(query) => f.map(Organization.toFieldsObj(_)) match {
        case Some(Right(searchFields)) => if(c == "true") Ok(JsObject(Seq("count" -> JsNumber(Organization.find(query).count))))
                                          else applySort(Organization.find(query,searchFields.dbfields))
        case None => if(c == "true") Ok(JsObject(Seq("count" -> JsNumber(Organization.find(query).count))))
                     else applySort(Organization.find(query))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidFields(error.clientOutput)))
      }
      case Left(sc) => sc.error match {
        case None => Ok(JsArray(Seq()))
        case Some(error) => BadRequest(Json.toJson(ApiError.InvalidQuery(error.clientOutput)))
      }
    }
  }

  /**
   * Returns a list of organizations visible to the organization in the request context
   *
   * @return
   */
  def getChildren(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort:Option[String]) = ApiActionRead { request =>
    doList(request.ctx.organization, q, f, c, sk, l, sort, childrenOnly =  true)
  }

  def getChildrenWithOrg(orgId:ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort:Option[String]) = ApiActionRead { request =>
    if(orgId == request.ctx.organization || Organization.isChild(request.ctx.organization,orgId))
      doList(orgId, q, f, c, sk, l, sort, childrenOnly = true)
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

  def isRoot = ApiAction { request =>
    if (request.ctx.organization == AppConfig.rootOrgId) Ok(JsObject(Seq("isRoot" -> JsBoolean(true))))
    else Ok(JsObject(Seq("isRoot" -> JsBoolean(false))))
  }
}
