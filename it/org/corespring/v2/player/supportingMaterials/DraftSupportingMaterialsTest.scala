package org.corespring.v2.player.supportingMaterials

import global.Global
import salat.Context
import org.corespring.drafts.item.models.DraftId
import org.corespring.drafts.item.{ DraftAssetKeys, ItemDraftHelper }
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.it.{ IntegrationSpecification, MultipartFormDataWriteable }
import org.corespring.models.item.resource.Resource
import org.corespring.services.item.ItemService
import org.specs2.execute.Result
import org.specs2.time.NoTimeConversions
import play.api.mvc.{ Call, SimpleResult }

import scala.concurrent.Future

class DraftSupportingMaterialsTest extends IntegrationSpecification with NoTimeConversions {
  import org.corespring.container.client.controllers.resources.routes.{ ItemDraft => Routes }

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelper
    with testDefaults {

    val helper = new ItemDraftHelper {
      override implicit def context: Context = main.context

      override def itemService: ItemService = main.itemService
    }

    val orgService = main.orgService
    val draftId = {
      val draftId = DraftId(itemId.id, user.userName, orgId)
      val org = orgService.findOneById(orgId).get
      helper.create(draftId, itemId, org)
    }

    def getHeadResource = helper.get(draftId).flatMap(_.change.data.supportingMaterials.headOption)

    def assertHeadResource(fn: Resource => Result) = getHeadResource match {
      case Some(r) => fn(r)
      case _ => failure("expected a resource")
    }

    def createHtmlMaterial: Future[SimpleResult] = {
      val call = Routes.createSupportingMaterial(draftId.itemId.toString)
      val createMaterial = makeJsonRequest(call, json)
      route(createMaterial)(writeableOf_AnyContentAsJson).get
    }

    override def after: Any = {
      super.after
      helper.delete(draftId)
    }
  }

  "create" should {

    "create a html based supporting material" in new scope {
      val result = createHtmlMaterial

      status(result) must_== CREATED

      getHeadResource match {
        case Some(Resource(_, materialName, Some("Rubric"), _)) => success
        case _ => failure("expected to get the head resource from supporting materials ")
      }
    }

    "create a binary supporting material" in new scope with withUploadFile {

      def filePath: String = s"/test-images/puppy.small.jpg"
      lazy val key = DraftAssetKeys.supportingMaterialFile(draftId, "binary-material", filename)

      override def after = {
        super.after
        println(s"deleting asset from s3: $key")
        ImageUtils.delete(key)
        fileCleanUp
      }

      val call = Routes.createSupportingMaterialFromFile(draftId.itemId.toString)

      val formData = Map("name" -> "binary-material", "materialType" -> "Rubric")

      val form = mkFormWithFile(formData)

      val req = makeFormRequest(call, form)

      route(req)(MultipartFormDataWriteable.writeableOf_multipartFormData).map { r =>
        status(r) must_== CREATED

        assertHeadResource { r =>
          r match {
            case Resource(_, "binary-material", Some("Rubric"), _) => success
            case _ => failure
          }
        }

        ImageUtils.list(key.replace(filename, ""))(0).contains(filename) must_== true
      }.getOrElse(failure("no result returned"))
    }
  }

  "delete" should {

    trait deleteScope extends scope {

      def deleteMaterial(name: String) = {
        val call = Routes.deleteSupportingMaterial(draftId.itemId.toString, name)
        val req = makeRequest(call)
        route(req).get
      }
    }

    "delete the supporting material" in new deleteScope {
      val result = for {
        _ <- createHtmlMaterial
        r <- deleteMaterial(materialName)
      } yield r

      status(result) must_== OK
      getHeadResource must beNone
    }
  }

  "updateFileContent" should {
    trait updateFileContentScope extends scope {
      def updateHtmlContent(content: String): Future[SimpleResult] = {
        val updateContent = Routes.updateSupportingMaterialContent(draftId.itemId.toString, materialName, "index.html")
        route(makeTextRequest(updateContent, content))(writeableOf_AnyContentAsText).get
      }
    }

    "update the file" in new updateFileContentScope {

      val result = for {
        _ <- createHtmlMaterial
        r <- updateHtmlContent("hi")
      } yield r

      status(result) must_== OK

      assertHeadResource { r =>
        r.defaultVirtualFile.map(_.content) must_== Some("hi")
      }
    }
  }

  "addAssetToSupportingMaterial" should {

    "upload the file to s3" in new addFileScope with scope {
      lazy val s3Key = DraftAssetKeys.supportingMaterialFile(draftId, materialName, filename)

      override def addFileCall: Call = Routes.addAssetToSupportingMaterial(draftId.itemId.toString, materialName)

      override def after = {
        super.after
        ImageUtils.delete(s3Key)
        fileCleanUp
      }

      val result = for {
        _ <- createHtmlMaterial
        r <- addFile
      } yield r

      status(result) must_== OK

      assertHeadResource { r =>
        r.files.length must_== 2
        r.files.exists(_.name.contains(filename)) must_== true
      }

      ImageUtils.list(s3Key.replace(filename, ""))(0).contains(filename) must_== true

    }
  }

  "deleteAssetFromSupportingMaterial" should {

    trait removeFileScope extends addFileScope with scope {

      override def addFileCall: Call = Routes.addAssetToSupportingMaterial(draftId.itemId.toString, materialName)

      lazy val s3Key = DraftAssetKeys.supportingMaterialFile(draftId, materialName, filename)

      def removeFile(path: String) = {
        val call = Routes.deleteAssetFromSupportingMaterial(draftId.itemId.toString, materialName, path)
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
        json <- Future.successful(contentAsJson(addFile))
        r <- removeFile((json \ "path").as[String])
      } yield r

      status(result) must_== OK

      assertHeadResource { r =>
        r.files.length must_== 1
        r.files.exists(_.name == filename) must_== false
      }

      ImageUtils.list(s3Key) must_== Nil

    }
  }
}
