package api.v1

import org.bson.types.ObjectId
import models.Organization
import play.api.libs.json.{JsValue, Json}
import controllers.auth.BaseApi
import com.novus.salat.dao.SalatSaveError
import api.ApiError
import play.api.mvc.Result

/**
 * The Organization API
 */
object OrganizationApi extends BaseApi {

  /**
   * Returns a list of organizations visible to the organization in the request context
   *
   * @return
   */
  def list() = ApiAction { request =>
    Ok(Json.toJson(Organization.findAllFor(request.ctx.organization).toList))
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
          //
          // todo: check if this org is visible to the caller?
          //
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
              val children = parseChildren(json, Seq.empty)
              val organization = Organization(newId, name.get, Some(request.ctx.organization), children)
              doSave(organization)
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
          val parentId = (json \ "parentId").asOpt[String] match {
            // if they passed a parentId then create an objectId
            case Some(str) if str.length > 0 => Some(new ObjectId(str))
            // if they passed an empty string remove the parent (todo: confirm with evan how this should work)
            case Some(str) if str.isEmpty => None
            // if nothing was passed, use the parentId in the original
            case None => original.parentId
          }
          val toUpdate = Organization( original.id, name, parentId, parseChildren(json, original.children))
          doSave(toUpdate)
        }
        case _ => jsonExpected
      }
    }).getOrElse(unknownOrganization)
  }

  /**
   * Deletes an organization
   */
  def deleteOrganization(id: ObjectId) = ApiAction { request =>
    if ( id.equals(request.ctx.organization)) {
      Forbidden(Json.toJson(ApiError.CantDeleteMainOrg))
    } else {
      Organization.findOneById(id) match {
        case Some(toDelete) => {
          Organization.removeById(id)
          Ok(Json.toJson(toDelete))
        }
        case _ => unknownOrganization
      }
    }
  }

  /**
   * Internal method to save an organization
   *
   * @param organization
   * @return
   */
  private def doSave(organization: Organization): Result = {
    try {
      Organization.save(organization)
      val newOrg = Organization.findOneById(organization.id)
      Ok(Json.toJson(newOrg))
    } catch {
      case ex: SalatSaveError => InternalServerError(Json.toJson(ApiError.CantSave))
    }
  }
}
