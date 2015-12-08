package org.corespring.v2.player.item

import org.bson.types.ObjectId
import org.corespring.it.helpers.ItemHelper
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.update.UpdateProfileIntegrationTest
import play.api.mvc.Call

class UpdateItemProfileIntegrationTest extends UpdateProfileIntegrationTest {

  override def getUpdateProfileCall(itemId: VersionedId[ObjectId], username: String): Call = {
    org.corespring.container.client.controllers.resources.routes.Item.saveSubset(itemId.toString, "profile")
  }

  override def initData(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Unit = {
    //nothing to do here
  }

  override def getUpdatedItem(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Item = {
    ItemHelper.get(itemId).get
  }
}
