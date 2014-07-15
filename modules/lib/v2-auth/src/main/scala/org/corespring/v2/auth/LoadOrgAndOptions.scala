package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait LoadOrgAndOptions {
  type OrgAndOpts = (ObjectId, PlayerOptions)
  def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts]
}
