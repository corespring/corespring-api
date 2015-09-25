package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.AddSupportingMaterialImageAndItem
import org.specs2.time.NoTimeConversions
import play.api.mvc.{AnyContentAsEmpty, Codec}

class LoadCatalogAssetIntegrationTest extends IntegrationSpecification with NoTimeConversions {

  "Load catalog asset" should {

    "load a supporting material asset" in new AddSupportingMaterialImageAndItem {
      //Note: the `lazy` is important here - otherwise there'll be a NPE in <AddSupportingMaterialImageAndItem>.
      override lazy val imagePath = "it/org/corespring/v2/player/load-image/puppy.png"
      override lazy val materialName = "Rubric"

      import org.corespring.container.client.controllers.resources.routes.Item
      val call = Item.getAssetFromSupportingMaterial(itemId.toString, materialName, fileName)
      val r = makeRequest(call, AnyContentAsEmpty)
      val future = route(r)(writeableOf_AnyContentAsEmpty(Codec.utf_8))
      future.map {
        result =>
          status(result) must_== OK
          contentAsBytes(result).length must_== fileBytes.length
      }.getOrElse(failure("route should have returned a result"))
    }
  }
}
