package org.corespring.v2player.integration.auth.requestTransformers

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.services.UserService
import org.corespring.v2
import org.corespring.v2.auth.services.OrgService
import play.api.mvc.RequestHeader

trait SessionOrgIdentity[B] extends v2.auth.SessionOrgIdentity[B] {

  override def userService: UserService = ???

  override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): B = ???

  override def orgService: OrgService = ???
}
