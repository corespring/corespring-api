package org.corespring.v2player.integration

import org.specs2.mutable.BeforeAfter
import org.corespring.test.helpers.models._
import play.api.Logger
import org.bson.types.ObjectId

package object scopes {

  val logger = Logger("it.scopes")

  trait data extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    def before: Any = {
      logger.debug(s"data ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    }

    def after: Any = {
      logger.trace(s"deleting db data..${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
      ApiClientHelper.delete(apiClient)
      OrganizationHelper.delete(orgId)
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
  }


  trait sessionData extends data{

    val sessionId : ObjectId = V2SessionHelper.create(itemId)

    override def before : Any = {
      super.before
    }

    override def after : Any = {
      super.after
      V2SessionHelper.delete(sessionId)
    }
  }

}
