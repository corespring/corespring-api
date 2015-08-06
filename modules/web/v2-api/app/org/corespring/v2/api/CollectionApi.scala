package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.services.ContentCollectionService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ Action, RequestHeader }

import scala.concurrent.{ Future, ExecutionContext }
import scalaz.Validation

class CollectionApi(
  contentCollectionService: ContentCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {
  override implicit def ec: ExecutionContext = v2ApiContext.context

  def getCollection(collectionId: ObjectId) = Action.async { request =>
    Future(Ok)
  }

  def list(q: Option[String] = None,
    f: Option[String] = None,
    c: Boolean = false,
    sk: Int = 0,
    l: Int = 50,
    sort: Option[String] = None) = Action.async { request =>
    Future(Ok)
  }
}
