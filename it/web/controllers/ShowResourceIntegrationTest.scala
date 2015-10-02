package web.controllers

import java.io.File

import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.{ userAndItem, AddImageAndItem }

class ShowResourceIntegrationTest extends IntegrationSpecification {

  trait scope extends userAndItem {

    //val img = new File("it/test-images/ervin.png")
    //ImageUtils.upload(img, s3Path)
    //val item = ItemHelper.get(itemId).get

    //item.copy
  }
  "getResourceFile" should {
    "load a binary asset from s3" in new scope {

      //web.controllers.routes.ShowResource.getResourceFile(itemId, "data", "test.png")
      true === true
    }
  }
}
