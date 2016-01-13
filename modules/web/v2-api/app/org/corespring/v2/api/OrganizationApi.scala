package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.services.{ OrgCollectionService }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Validation

class OrganizationApi(
  orgCollectionService: OrgCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  /**
    * [{ name: x, id: x, permission: x} ]
    * @param collectionId
    * @return
    */
  def getOrgsWithSharedCollection(collectionId: ObjectId) = futureWithIdentity { (identity, request) =>
    Future {
      val result = orgCollectionService.ownsCollection(identity.org, collectionId).map { _ =>
        orgCollectionService.getOrgsWithAccessTo(collectionId).filterNot(_.id == identity.org.id)
      }

      def toNameAndPermission(o: Organization): Option[JsValue] = {
        o.contentcolls.find(_.collectionId == collectionId).map { r =>
          val permission : String = Permission.fromLong(r.pval).map(_.name).getOrElse("none")
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