package org.corespring.importing

import java.io.File

import org.corespring.it.{MultipartFormDataWriteable, IntegrationSpecification}
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes.{SessionRequestBuilder, userAndItem}
import org.corespring.v2.player.supportingMaterials.withUploadFile
import org.specs2.specification.Scope
import play.api.test.FakeRequest

class ItemImportIntegrationTest extends IntegrationSpecification{


  trait upload extends Scope
  with userAndItem
  with SessionRequestBuilder
  with SecureSocialHelper
  with withUploadFile{
    override lazy val filePath: String = "/item-import/item-one.zip"
  }

  "upload" should {

    //TODO: create the zip on the fly
    "create a new item" in new upload{
      val call = org.corespring.importing.controllers.routes.ItemImportController.upload()

      val form = mkFormWithFile(Map.empty)

      val request = makeFormRequest(call, form)

      route(request)(MultipartFormDataWriteable.writeableOf_multipartFormData).map { result =>
        logger.info(contentAsString(result))
        //Should be CREATED
        status(result) === OK
      }.getOrElse{ko}
    }
  }


}
