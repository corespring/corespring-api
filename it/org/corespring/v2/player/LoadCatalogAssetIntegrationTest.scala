package org.corespring.v2.player

import org.corespring.container.client.controllers.apps.routes.Catalog
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.AddSupportingMaterialImageAndItem
import play.api.test.FakeRequest

class LoadCatalogAssetIntegrationTest extends IntegrationSpecification {

  "Load catalog asset" should {

    "load a supporting material asset" in
      new AddSupportingMaterialImageAndItem {
        override lazy val imagePath = "it/org/corespring/v2/player/load-image/puppy.png"
        override lazy val materialName = "Rubric"

        val call = Catalog.getSupportingMaterialFile(itemId.toString, s"$materialName/$fileName")
        route(FakeRequest(call.method, call.url)).map {
          result =>
            status(result) must_== OK
            contentAsBytes(result).length must_== fileBytes.length
        }.getOrElse(failure("route should have returned a result"))
      }
  }
}
