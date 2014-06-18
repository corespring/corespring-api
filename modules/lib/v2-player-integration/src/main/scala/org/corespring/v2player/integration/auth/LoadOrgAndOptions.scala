package org.corespring.v2player.integration.auth

import org.bson.types.ObjectId
import org.corespring.v2player.integration.cookies.PlayerOptions
import play.api.mvc.RequestHeader

import scalaz.Validation

trait LoadOrgAndOptions {
  type OrgAndOpts = (ObjectId, PlayerOptions)
  def getOrgIdAndOptions(request: RequestHeader): Validation[String, (ObjectId, PlayerOptions)]
}

