package org.corespring.v2.auth

import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait LoadOrgAndOptions {
  def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts]
}
