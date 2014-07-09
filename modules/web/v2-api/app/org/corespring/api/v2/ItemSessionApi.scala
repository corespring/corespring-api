package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.{ OrgRequest, V2ApiActions }
import org.corespring.api.v2.errors.Errors.{ cantFindSession, generalError }
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.api.v2.services.PermissionService
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.services.OrgService
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Success, Validation }

trait ItemSessionApi extends V2Api {

  def sessionService: MongoService

  def actions: V2ApiActions[AnyContent]

  def orgService: OrgService

  def permissionService: PermissionService[Organization, JsValue]

  def create(itemId: VersionedId[ObjectId]) = actions.orgAction(BodyParsers.parse.anyContent) {

    request: OrgRequest[AnyContent] =>
      Future {

        def createSessionJson(vid: VersionedId[ObjectId]) = Json.obj(
          "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
          "itemId" -> JsString(vid.toString))

        val result: Validation[V2ApiError, ObjectId] = for {
          json <- Success(createSessionJson(itemId))
          org <- orgService.org(request.orgId).toSuccess(generalError(BAD_REQUEST, s"Can't find org: ${request.orgId}"))
          permission <- toValidation(permissionService.create(org, json))
          sessionId <- sessionService.create(json).toSuccess(generalError(BAD_REQUEST, s"Error creating session with json: ${json}"))
        } yield sessionId

        validationToResult[ObjectId](oid => Ok(Json.obj("id" -> oid.toString)))(result)
      }
  }

  def get(sessionId: String) = actions.orgAction(BodyParsers.parse.anyContent) { request: OrgRequest[AnyContent] =>
    Future {

      val out: Validation[V2ApiError, JsValue] = for {
        s <- sessionService.load(sessionId).toSuccess(cantFindSession(sessionId))
        org <- orgService.org(request.orgId).toSuccess(generalError(BAD_REQUEST, s"Can't find org: ${request.orgId}"))
        permission <- toValidation(permissionService.get(org, s))
      } yield s

      validationToResult[JsValue](json => Ok(json))(out)
    }
  }
}