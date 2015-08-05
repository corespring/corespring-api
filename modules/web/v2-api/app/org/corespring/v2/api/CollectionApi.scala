package org.corespring.v2.api

import org.corespring.services.ContentCollectionService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scalaz.Validation

class CollectionApi(
  contentCollectionService: ContentCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {
  override implicit def ec: ExecutionContext = ?
}
