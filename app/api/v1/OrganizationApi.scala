package api.v1

import org.bson.types.ObjectId
import models.Organization
import play.api.libs.json.{JsValue, Json}
import controllers.auth.BaseApi
import com.novus.salat.dao.{SalatDAOUpdateError, SalatSaveError}
import api._
import play.api.mvc.Result
import controllers.services.OrgService
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatSaveError
import play.api.mvc.Result
import play.api.Logger
import com.mongodb.util.JSONParseException
import scala.Left
import scala.Some
import scala.Right

/**
 * The Organization API
 */
object OrganizationApi extends BaseApi {

  /**
   * Returns a list of organizations visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    Logger.debug("q in controller = " + q)

    try {
      import com.mongodb.casbah.Imports._

      val query = q.map( QueryHelper.parse(_, Organization.queryFields) )
      query.map( Organization.find(_) ) match {
        case Some(cursor) => {
          cursor.skip(sk)
          cursor.limit(l)
          Ok(
            // I'm using a String for c because if I use a boolean I need to pass 0 or 1 from the command line for Play to parse the boolean.
            // I think using "true" or "false" is better
            if ( c.equalsIgnoreCase("true") ) CountResult.toJson(cursor.count) else Json.toJson(cursor.toList)
          )
        }
        case None => Ok(Json.toJson(OrgService.getTree(request.ctx.organization)))
      }
    } catch {
      case e: JSONParseException => BadRequest(Json.toJson(ApiError.InvalidQuery))
      case ife: InvalidFieldException => BadRequest(Json.toJson(ApiError.UnknownFieldOrOperator.format(ife.field)))
    }
  }

  /**
   * Returns an Organization by its id
   *
   * @param id The organization id
   * @return
   */
  def getOrganization(id: ObjectId) = ApiAction { request =>
    Organization.findOneById(id) match {
        case Some(org) =>  {
          if (request.ctx.organization == org.id){
            Ok(Json.toJson(org))
          }else{
            OrgService.getTree(request.ctx.organization).find(_ == org.id) match {
              case Some(_) => Ok(Json.toJson(org))
              case None => Unauthorized
            }
          }
          Ok(Json.toJson(org))
        }
        case _ => NotFound
    }
  }

  /**
   * Returns the organization in the request context
   *
   * @return
   */
  def getDefaultOrganization = ApiAction { request =>
    Ok(Json.toJson(Organization.findOneById( request.ctx.organization )))
  }

  /**
   * Creates an organization
   *
   * @return
   */
  def createOrganization = ApiAction { request =>
    //
    // deserialize the organization and assign the caller's organization as the parent
    // todo: confirm with evan this is what we want.
    //
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val newId = new ObjectId
            val name = (json \ "name").asOpt[String]
            if ( name.isEmpty ) {
              BadRequest( Json.toJson(ApiError.OrgNameMissing))
            } else {
              val optParent:Option[ObjectId] = (json \ "parent_id").asOpt[String].map(new ObjectId(_))
              val organization = Organization(name.get)
              Organization.insert(organization,optParent) match {
                case Right(org) => Ok(Json.toJson(org))
                case Left(e) => InternalServerError(Json.toJson(e))
              }
            }
          }
        }
      }
      case _ => jsonExpected
    }
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
            case Left(e) => InternalServerError(Json.toJson(e))
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
            case Left(e) => InternalServerError(Json.toJson(e))
          }
        }
        case _ => unknownOrganization
      }
    }
  }

}
