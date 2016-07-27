package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.{DisplayConfig, ColorPalette, Organization}
import org.corespring.models.auth.Permission
import org.corespring.services.{OrganizationService, OrgCollectionService}
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{noJson, invalidJson, generalError}
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{JsSuccess, JsArray, JsValue, Json}
import play.api.mvc.RequestHeader
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.errors.Errors.generalError
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{Failure, Success, Validation}


class OrganizationApi(
  organizationService: OrganizationService,
  actions: V2Actions,
  orgCollectionService: OrgCollectionService,
  v2ApiContext: V2ApiExecutionContext) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  /**
   * [{ name: x, id: x, permission: x} ]
   * @param collectionId
   * @return
   */
  def getOrgsWithSharedCollection(collectionId: ObjectId) = actions.Org.async { request =>
    Future {
      val result = orgCollectionService.ownsCollection(request.org, collectionId).map { _ =>
        orgCollectionService.getOrgsWithAccessTo(collectionId).filterNot(_.id == request.org.id)
      }

      def toNameAndPermission(o: Organization): Option[JsValue] = {
        o.contentcolls.find(_.collectionId == collectionId).map { r =>
          val permission: String = Permission.fromLong(r.pval).map(_.name).getOrElse("none")
          Json.obj(
            "name" -> o.name,
            "id" -> o.id.toString,
            "permission" -> permission)
        }
      }

      result.bimap(e => generalError(e.message), result => JsArray(result.flatMap(toNameAndPermission))).toSimpleResult()
    }
  }

  def getDisplayConfig = actions.Org.async { request =>
    implicit val Writes = DisplayConfig.Writes
    Future.successful(Ok(Json.prettyPrint(Json.toJson(request.org.displayConfig))))
  }

  def updateDisplayConfig = actions.Org.async { request =>
    implicit val Writes = DisplayConfig.Writes
    implicit val Reads = new DisplayConfig.Reads(request.org.displayConfig)

    Future.successful(request.body.asJson match {
      case Some(json) => Json.fromJson[DisplayConfig](json) match {
        case JsSuccess(displayConfig, _) =>
          organizationService.save(request.org.copy(displayConfig = displayConfig)).v2Error.map(org =>
            Json.toJson(org.displayConfig)).toSimpleResult()
        case _ => invalidJson(request.body.asText.getOrElse("")).toResult
      }
      case _ => noJson.toResult
    })
  }

  def displayConfigDefault = actions.Org.async { request =>
    implicit val Writes = DisplayConfig.Writes
    Future.successful(Ok(Json.prettyPrint(Json.toJson(DisplayConfig.default))))
  }

}