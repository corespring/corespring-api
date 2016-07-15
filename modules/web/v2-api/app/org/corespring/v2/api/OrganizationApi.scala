package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.services.OrgCollectionService
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.errors.Errors.generalError
import play.api.libs.json.{ JsArray, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

class OrganizationApi(
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
}