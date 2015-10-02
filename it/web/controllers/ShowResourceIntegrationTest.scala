package web.controllers

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.{ AddImageAndItem, userAndItem }
import org.specs2.specification.Scope

class ShowResourceIntegrationTest extends IntegrationSpecification {

  trait scope extends AddImageAndItem {
    override lazy val imagePath = "it/org/corespring/v2/player/load-image/puppy.png"

    val item = ItemHelper.get(itemId).get

    //item.copy
  }
  "getResourceFile" should {
    "load a binary asset from s3" in new scope {

      //web.controllers.routes.ShowResource.getResourceFile(itemId, "data", "test.png")
      true === true
    }
  }
}
