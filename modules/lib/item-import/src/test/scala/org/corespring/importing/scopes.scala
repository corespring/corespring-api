package org.corespring

import _root_.play.api.Logger
import org.specs2.mutable.BeforeAfter
import org.corespring.test.helpers.models._

trait orgWithAccessToken extends BeforeAfter {

  val logger = Logger("scopes")

  val orgId = OrganizationHelper.create("org")
  val apiClient = ApiClientHelper.create(orgId)
  val user = UserHelper.create(orgId, "test_user")
  val accessToken = AccessTokenHelper.create(orgId, "test_user")

  println(s"[accessToken] is: $accessToken")

  def before: Any = {
    logger.debug(s"[before] apiClient ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
  }

  def after: Any = {
    println("[orgWithAccessToken] after")
    logger.trace(s"[after] deleting db data: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    ApiClientHelper.delete(apiClient)
    OrganizationHelper.delete(orgId)
    AccessTokenHelper.delete(accessToken)
    UserHelper.delete(user.id)
  }
}
