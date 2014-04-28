package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.test.helpers.models._
import org.specs2.mutable.BeforeAfter
import play.api.Logger

package object scopes {

  val logger = Logger("it.scopes")

  trait orgWithAccessToken extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val user = UserHelper.create(orgId, "test_user")
    val accessToken = AccessTokenHelper.create(orgId, "test_user")

    def before: Any = {
      logger.debug(s"[before] apiClient ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    }

    def after: Any = {
      logger.trace(s"[after] deleting db data: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
      ApiClientHelper.delete(apiClient)
      OrganizationHelper.delete(orgId)
      AccessTokenHelper.delete(accessToken)
      UserHelper.delete(user.id)
    }
  }

  trait orgWithAccessTokenAndItem extends orgWithAccessToken {

    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    override def after: Any = {
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
  }

  trait sessionData extends orgWithAccessToken {
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)
    val sessionId: ObjectId = V2SessionHelper.create(itemId)

    override def before: Any = {
      super.before
    }

    override def after: Any = {
      super.after
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
      V2SessionHelper.delete(sessionId)
    }
  }

  trait user extends BeforeAfter {

    val orgId = OrganizationHelper.create("my-org")
    val user = UserHelper.create(orgId)
    val collectionId = CollectionHelper.create(orgId)

    def before: Any = {
    }

    def after: Any = {

      UserHelper.delete(user.id)
      OrganizationHelper.delete(orgId)
      CollectionHelper.delete(collectionId)
    }
  }

}
