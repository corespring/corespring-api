package org.corespring.v2.player.supportingMaterials

import org.corespring.it.{ IntegrationSpecification, MultipartFormDataWriteable }
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.services.item.ItemAssetKeys
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ ImageUtils, SessionRequestBuilder, userAndItem }
import org.specs2.execute.Result
import org.specs2.time.NoTimeConversions
import play.api.mvc.{ Call, SimpleResult }

import scala.concurrent.Future

class ItemSupportingMaterialsTest extends IntegrationSpecification with NoTimeConversions {
  import org.corespring.container.client.controllers.resources.routes.{ Item => ItemRoutes }

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelpers
    with Helpers.requestToFuture
    with testDefaults {

    def getHeadResource = ItemHelper.get(itemId).flatMap(_.supportingMaterials.headOption)

    def assertHeadResource(fn: Resource => Result) = getHeadResource match {
      case Some(r) => fn(r)
      case _ => failure("expected a resource")
    }

    def createHtmlMaterial: Future[SimpleResult] = {
      val call = ItemRoutes.createSupportingMaterial(itemId.toString)
      val createMaterial = makeJsonRequest(call, json)
      futureResult(createMaterial)
    }
  }

  "create" should {

    "create a html based supporting material" in new scope {
      val result = createHtmlMaterial
      status(result) === CREATED

      getHeadResource match {
        case Some(Resource(_, materialName, Some("Rubric"), _)) => success
        case _ => failure("expected to get the head resource from supporting materials ")
      }
    }

    "create a binary supporting material" in new scope with withUploadFile {

      def filePath: String = s"it/org/corespring/v2/player/load-image/puppy.small.jpg"
      lazy val key = ItemAssetKeys.supportingMaterialFile(itemId, "binary-material", filename)

      override def after = {
        super.after
        println(s"deleting asset from s3: $key")
        ImageUtils.delete(key)
        fileCleanUp
      }

      val call = ItemRoutes.createSupportingMaterialFromFile(itemId.toString)

      val formData = Map("name" -> "binary-material", "materialType" -> "Rubric")

      val form = mkFormWithFile(formData)

      val req = makeFormRequest(call, form)

      route(req)(MultipartFormDataWriteable.writeableOf_multipartFormData).map { r =>
        status(r) === CREATED

        assertHeadResource { r =>
          r match {
            case Resource(_, "binary-material", Some("Rubric"), _) => success
            case _ => failure
          }
        }

        ImageUtils.list(key) === Seq(key)
      }.getOrElse(failure("no result returned"))
    }
  }

  "delete" should {

    trait deleteScope extends scope {

      def deleteMaterial(name: String) = {
        val call = ItemRoutes.deleteSupportingMaterial(itemId.toString, name)
        val req = makeRequest(call)
        futureResult(req)
      }
    }

    "delete the supporting material" in new deleteScope {
      val result = for {
        _ <- createHtmlMaterial
        r <- deleteMaterial(materialName)
      } yield r

      status(result) === OK
      getHeadResource must beNone
    }
  }

  "updateFileContent" should {
    trait updateFileContentScope extends scope {
      def updateHtmlContent(content: String): Future[SimpleResult] = {
        val updateContent = ItemRoutes.updateSupportingMaterialContent(itemId.toString, materialName, "index.html")
        futureResult(makeTextRequest(updateContent, content))
      }
    }

    "update the file" in new updateFileContentScope {

      val result = for {
        _ <- createHtmlMaterial
        r <- updateHtmlContent("hi")
      } yield r

      status(result) === OK

      assertHeadResource { r =>
        r.defaultVirtualFile.map(_.content) must_== Some("hi")
      }
    }
  }

  "addAssetToSupportingMaterial" should {

    "upload the file to s3" in new addFileScope with scope {
      lazy val s3Key = ItemAssetKeys.supportingMaterialFile(itemId, materialName, filename)

      override def addFileCall: Call = ItemRoutes.addAssetToSupportingMaterial(itemId.toString, materialName)

      override def after = {
        super.after
        ImageUtils.delete(s3Key)
        fileCleanUp
      }

      val result = for {
        _ <- createHtmlMaterial
        r <- addFile
      } yield r

      println(contentAsString(result))
      status(result) === OK

      assertHeadResource { r =>
        r.files.length === 2
        r.files.exists(_.name == filename) === true
      }

      ImageUtils.list(s3Key) === Seq(s3Key)

    }
  }

  "deleteAssetFromSupportingMaterial" should {

    trait removeFileScope extends addFileScope with scope {

      lazy val s3Key = ItemAssetKeys.supportingMaterialFile(itemId, materialName, filename)

      override def addFileCall: Call = ItemRoutes.addAssetToSupportingMaterial(itemId.toString, materialName)

      def removeFile = {
        val call = ItemRoutes.deleteAssetFromSupportingMaterial(itemId.toString, materialName, filename)
        val req = makeRequest(call)
        futureResult(req)
      }
    }

    "delete the asset from the db and s3" in new removeFileScope {

      override def after = {
        super.after
        fileCleanUp
      }

      val result = for {
        _ <- createHtmlMaterial
        _ <- addFile
        r <- removeFile
      } yield r

      println(contentAsString(result))
      status(result) === OK

      assertHeadResource { r =>
        r.files.length === 1
        r.files.exists(_.name == filename) === false
      }

      ImageUtils.list(s3Key) === Nil

    }

  }
}
