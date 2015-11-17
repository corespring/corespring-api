package org.corespring.v2.player.itemDraft

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.item.ItemDraftHelper
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.helpers.OrganizationHelper
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.player.update.UpdateProfileIntegrationTest
import play.api.mvc.Call

class UpdateItemDraftProfileIntegrationTest extends UpdateProfileIntegrationTest {

  val itemDraftHelper = new ItemDraftHelper {
    override def itemService: ItemService = bootstrap.Main.itemService
    override implicit def context: Context = bootstrap.Main.context
  }

  override def getUpdateProfileCall(itemId: VersionedId[ObjectId], username: String): Call = {
    val draftIdString = DraftId(itemId.id, username, ObjectId.get).toIdString
    org.corespring.container.client.controllers.resources.routes.ItemDraft.saveSubset(draftIdString, "profile")
  }

  override def getUpdatedItem(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Item = {
    val draftId = DraftId(itemId.id, username, orgId)
    itemDraftHelper.get(draftId).get.change.data
  }

  override def initData(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Unit = {
    val draftId = DraftId(itemId.id, username, orgId)
    val org = OrganizationHelper.service.findOneById(orgId).get
    itemDraftHelper.create(draftId, itemId, org)
  }
}
