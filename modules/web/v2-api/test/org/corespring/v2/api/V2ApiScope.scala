package org.corespring.v2.api

import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scalaz.{ Validation }

trait V2ApiScope {

  implicit val v2ApiContext = V2ApiExecutionContext(ExecutionContext.global)

  def orgAndOpts: Validation[V2Error, OrgAndOpts]

  protected def getOrgAndOptionsFn = (request: RequestHeader) => orgAndOpts
}
