package org.corespring.v2.player.supportingMaterials

import java.io.File

import org.apache.commons.io.FileUtils
import org.corespring.it.{ IntegrationSpecification, MultipartFormDataWriteable }
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.services.item.ItemAssetKeys
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ ImageUtils, SessionRequestBuilder, userAndItem }
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart

class ItemSupportingMaterialsTest extends IntegrationSpecification {
  import org.corespring.container.client.controllers.resources.routes.{ Item => ItemRoutes }

  trait baseScope extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {
    def getHeadResource = ItemHelper.get(itemId).flatMap(_.supportingMaterials.headOption)

  }

  "create" should {

    "create a html based supporting material" in new baseScope {

      val json = Json.obj(
        "name" -> "new material",
        "materialType" -> "Rubric",
        "html" -> "<div>Hi</div>")

      val call = ItemRoutes.createSupportingMaterial(itemId.toString)
      val req = makeJsonRequest(call, json)
      route(req).map { r =>
        status(r) === CREATED
        getHeadResource match {
          case Some(Resource(_, "new material", Some("Rubric"), _)) => success
          case _ => failure
        }
      }.getOrElse(failure("no result returned"))
    }

    "create a binary supporting material" in new baseScope {

      val path = "it/org/corespring/v2/player/load-image/puppy.small.jpg"

      /**
       * Note - we need to create a temporary file as it is going to be deleted as part of the upload.
       */
      lazy val fileToUpload = {
        val f = new File(path)
        require(f.exists, s"$path doesn't exist?")
        val dest = new File(path.replace("jpg", "tmp.jpg"))
        println(s"dest: $dest")
        FileUtils.copyFile(f, dest)
        dest
      }

      def mkForm(dataParts: Map[String, Seq[String]] = Map.empty,
        files: Seq[MultipartFormData.FilePart[Files.TemporaryFile]] = Seq.empty) = {
        MultipartFormData[Files.TemporaryFile](dataParts, files, badParts = Seq.empty, missingFileParts = Seq.empty)
      }

      def mkFormWithFile(params: Map[String, String], filename: String = "image.png", contentType: Option[String] = None) = {
        val files = Seq(FilePart[Files.TemporaryFile]("file", filename, contentType, Files.TemporaryFile(fileToUpload)))
        val dataParts = params.mapValues(Seq(_))
        mkForm(dataParts, files)
      }

      lazy val key = ItemAssetKeys.supportingMaterialFile(itemId, "binary-material", "puppy.jpg")

      override def after = {
        super.after
        println(s"deleting asset from s3: $key")
        ImageUtils.delete(key)
        fileToUpload.delete()
      }

      val call = ItemRoutes.createSupportingMaterialFromFile(itemId.toString)
      val form = mkFormWithFile(
        Map(
          "name" -> "binary-material",
          "materialType" -> "Rubric"),
        filename = "puppy.jpg",
        contentType = Some("image/jpeg"))

      val req = makeFormRequest(call, form)

      route(req)(MultipartFormDataWriteable.writeableOf_multipartFormData).map { r =>
        status(r) === CREATED
        getHeadResource match {
          case Some(Resource(_, "binary-material", Some("Rubric"), _)) => success
          case _ => failure
        }
        ImageUtils.list(key) === Seq(key)
      }.getOrElse(failure("no result returned"))
    }
  }
}
