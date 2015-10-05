package web.controllers

import java.io.File

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.{ ItemHelper, SecureSocialHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ Resource, StoredFile, VirtualFile }

class ShowResourceIntegrationTest extends IntegrationSpecification {

  trait rootBinary extends userAndItem with SessionRequestBuilder with SecureSocialHelper {
    def s3Path: String
    def item: Item
    val path = "it/test-images/ervin.png"
    val img = new File(path)
    lazy val bytes = ImageUtils.imageData(path)
    ImageUtils.upload(img, s3Path)
    logger.info(s"save item: $item")
    ItemHelper.update(item)
  }

  trait dataBinary extends rootBinary {
    lazy val s3Path: String = StoredFile.storageKey(itemId.id, itemId.version.get, "data", "ervin.png")
    override lazy val item: Item = {
      val i = ItemHelper.get(itemId).get
      val files = i.data.get.files :+ StoredFile("ervin.png", "image/png", false, s3Path)
      val updated = i.data.get.copy(files = files)
      val update = i.copy(data = Some(updated))
      update
    }
  }

  trait supportingMaterialBinary extends rootBinary {
    lazy val s3Path: String = StoredFile.storageKey(itemId.id, itemId.version.get, "rubric", "ervin.png")
    override lazy val item: Item = {
      val i = ItemHelper.get(itemId).get
      val file = StoredFile("ervin.png", "image/png", false, s3Path)
      val resource = Resource(id = Some(ObjectId.get), name = "rubric", materialType = Some("Rubric"), files = Seq(file))
      val out = i.copy(supportingMaterials = Seq(resource))
      out
    }
  }

  trait virtual extends userAndItem with SessionRequestBuilder with SecureSocialHelper {
    val item = ItemHelper.get(itemId).get
    val vf = VirtualFile("ervin.txt", "text/txt", false, "Ervin!")
    val files = item.data.get.files :+ vf
    val updated = item.data.get.copy(files = files)
    val updatedItem = item.copy(data = Some(updated))
    ItemHelper.update(updatedItem)
  }

  "getResourceFile" should {
    "load a binary asset from s3, in the data resource" in new dataBinary {

      val call = web.controllers.routes.ShowResource.getResourceFile(itemId.toString, "data", "ervin.png")
      val req = makeRequest(call)
      route(req).map { r =>
        status(r) === OK
        contentAsBytes(r) === bytes
      }
    }

    "load a binary asset from s3, in a supporting material resource" in new supportingMaterialBinary {

      val call = web.controllers.routes.ShowResource.getResourceFile(itemId.toString, "rubric", "ervin.png")
      val req = makeRequest(call)
      route(req).map { r =>
        status(r) === OK
        contentAsBytes(r) === bytes
      }
    }

    "load a virtual file from the db, in the data resource" in new virtual {
      val call = web.controllers.routes.ShowResource.getResourceFile(itemId.toString, "data", "ervin.txt")
      val req = makeRequest(call)
      route(req).map { r =>
        status(r) === OK
        contentAsString(r) === vf.content
      }
    }
  }
}
