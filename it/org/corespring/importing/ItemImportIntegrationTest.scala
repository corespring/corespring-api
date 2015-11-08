package org.corespring.importing

import java.io.File

import org.apache.commons.io.FileUtils
import org.corespring.it.{ MultipartFormDataWriteable, IntegrationSpecification }
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.supportingMaterials.withUploadFile
import org.specs2.mutable.After
import org.specs2.specification.Scope
import play.api.test.FakeRequest

class ItemImportIntegrationTest extends IntegrationSpecification {

  trait upload extends Scope
    with userAndItem
    with SessionRequestBuilder
    with SecureSocialHelper
    with withUploadFile
    with After {

    val basePath = "/item-import/item-one"

    private def uri(s: String) = this.getClass.getResource(s).toURI
    override lazy val filePath: String = {
      val dir = new File(uri(basePath))
      val zip = new File(s"${dir.getAbsolutePath}.zip")
      import org.zeroturnaround.zip.ZipUtil
      ZipUtil.pack(dir, zip)
      s"$basePath.zip"
    }

    override def after = {
      super.after
      logger.debug(s"delete zip: $basePath.zip")
      FileUtils.deleteQuietly(new File(uri(s"$basePath.zip")))
    }
  }

  "upload" should {

    //TODO: create the zip on the fly
    "create a new item" in new upload {

      val call = org.corespring.importing.controllers.routes.ItemImportController.upload()

      val form = mkFormWithFile(Map.empty)

      val request = makeFormRequest(call, form)

      route(request)(MultipartFormDataWriteable.writeableOf_multipartFormData).map { result =>
        logger.info(contentAsString(result))
        //TODO: Should be CREATED
        status(result) === OK
        val vidString = contentAsString(result)
        val vid = VersionedId(vidString).get
        val item = bootstrap.Main.itemService.findOneById(vid)
        item.isDefined must_== true
        val assetKey = bootstrap.Main.itemAssetKeys.file(vid, "ervin.png")
        val metadata = bootstrap.Main.s3.getObjectMetadata(bootstrap.Main.bucket.bucket, assetKey)
        metadata.getContentType must_== "image/png"
      }.getOrElse { ko }
    }
  }

}
