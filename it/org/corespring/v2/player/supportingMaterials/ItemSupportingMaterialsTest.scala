package org.corespring.v2.player.supportingMaterials

import java.io.File

import org.apache.commons.io.FileUtils
import org.corespring.it.{ IntegrationSpecification, MultipartFormDataWriteable }
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.services.item.ItemAssetKeys
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ ImageUtils, SessionRequestBuilder, userAndItem }
import org.specs2.time.NoTimeConversions
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Await
import scala.concurrent.duration._

class ItemSupportingMaterialsTest extends IntegrationSpecification with NoTimeConversions {
  import org.corespring.container.client.controllers.resources.routes.{ Item => ItemRoutes }

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {
    def getHeadResource = ItemHelper.get(itemId).flatMap(_.supportingMaterials.headOption)
  }

  val materialName = "new-material"

  val json = Json.obj(
    "name" -> materialName,
    "materialType" -> "Rubric",
    "html" -> "<div>Hi</div>")

  "create" should {

    "create a html based supporting material" in new scope {

      val call = ItemRoutes.createSupportingMaterial(itemId.toString)
      val req = makeJsonRequest(call, json)
      route(req).map { r =>
        status(r) === CREATED

        getHeadResource match {
          case Some(Resource(_, materialName, Some("Rubric"), _)) => success
          case _ => failure("expected to get the head resource from supporting materials ")
        }
      }.getOrElse(failure("no result returned"))
    }

    "create a binary supporting material" in new scope {

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

  "updateFileContent" should {
    trait updateFileContentScope extends scope
    "update the file" in new updateFileContentScope {
      val call = ItemRoutes.createSupportingMaterial(itemId.toString)
      val createMaterial = makeJsonRequest(call, json)

      route(createMaterial).map(f => Await.result(f, 1.second))
      val updateContent = ItemRoutes.updateSupportingMaterialContent(itemId.toString, materialName, "index.html")

      route(makeTextRequest(updateContent, "new content")).map { f =>
        println(s"error: ${contentAsString(f)}")
        status(f) === 200

        ItemHelper.get(itemId).flatMap(_.supportingMaterials.headOption).map { r =>
          println(s" ----> ${r.defaultVirtualFile}")
          r.defaultVirtualFile.map(_.content) must_== Some("new content")
        }.getOrElse(failure("expected to find the updated resource"))
      }.getOrElse(failure("expected to get a result back"))
    }
  }
}
