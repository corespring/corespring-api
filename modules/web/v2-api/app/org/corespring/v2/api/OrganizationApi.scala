package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ ContentCollectionService, OrganizationService }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Validation

class OrganizationApi(service: OrganizationService,
  contentCollectionService: ContentCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  jsonFormatting: JsonFormatting,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  import jsonFormatting.writeOrg

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def getOrgsWithSharedCollection(collectionId: ObjectId) = futureWithIdentity { (identity, request) =>
    Future {
      val result = contentCollectionService.ownsCollection(identity.org, collectionId).map { _ =>
        service.getOrgsWithAccessTo(collectionId)
      }

      result.bimap(e => generalError(e.message), orgs => Json.toJson(orgs)).toSimpleResult()
    }
  }
}