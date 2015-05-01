package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.v2.player.scopes.AddSupportingMaterialImageAndItem
import org.corespring.container.client.controllers.apps.routes.Catalog
import play.api.test.FakeRequest

class LoadCatalogAssetTest extends IntegrationSpecification {


  "Load catalog asset" should {

    "load a supporting material asset" in
      new AddSupportingMaterialImageAndItem("it/org/corespring/v2/player/load-image/puppy.png", "Rubric"){
      val call = Catalog.getSupportingMaterialFile(itemId.toString, s"$materialName/$fileName")
      route(FakeRequest(call.method, call.url)).map{
        result =>
          status(result) must_== OK
          contentAsBytes(result).length must_== fileBytes.length
      }.getOrElse(failure("route should have returned a result"))
    }
  }
}
