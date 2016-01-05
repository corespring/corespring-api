package org.corespring.api.v1

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.{TokenRequestBuilder, orgWithAccessTokenAndItem, userAndItem}
import org.corespring.models.item.Item.QtiResource
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.VirtualFile
import org.corespring.models.json.item.resource.BaseFileFormat
import org.specs2.specification.Scope
import play.api.libs.json.Json

class ResourceApiIntegrationTest extends IntegrationSpecification {


  trait scope extends orgWithAccessTokenAndItem with TokenRequestBuilder{

    lazy val Routes = org.corespring.api.v1.routes.ResourceApi
  }

  def mkQti(content:String) = <assessmentItem><itemBody>{content}</itemBody></assessmentItem>

  "updateDataFile" should {

    "AC-291 - update and transform even though the item has an existing `playerDefinition` property" in new scope {
      val item = ItemHelper.get(itemId).get
      val update = item.copy(playerDefinition = Some(PlayerDefinition.apply("original")))
      ItemHelper.update(update)
      val call = Routes.updateDataFile(itemId.toString, "qti.xml")
      val file = new VirtualFile(
        name = QtiResource.QtiXml,
        contentType = "text/xml",
        content = mkQti("new-content").toString)
      implicit val f = BaseFileFormat
      val json = Json.toJson(file)
      val request = makeJsonRequest(call, json)
      val result = route(request)(writeableOf_AnyContentAsJson).get
      status(result) must_== OK
      val updatedItem = ItemHelper.get(itemId).get
      updatedItem.playerDefinition.map(_.xhtml) must_== Some("<div class=\"item-body qti\">new-content</div>")
    }
  }
}
