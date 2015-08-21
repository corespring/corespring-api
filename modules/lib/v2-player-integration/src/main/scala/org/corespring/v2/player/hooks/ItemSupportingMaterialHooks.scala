package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.R
import org.corespring.container.client.hooks._
import org.corespring.platform.core.models.item.resource.{ VirtualFile, BaseFile, StoredFile, Resource }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.cantParseItemId
import play.api.libs.json.JsValue
import play.api.mvc.{ SimpleResult, RequestHeader }
import scalaz.Scalaz._

trait MaterialToResource {

  private def toFile[F <: File](sm: CreateNewMaterialRequest[F]): BaseFile = sm match {
    case CreateBinaryMaterial(_, _, binary) => {
      StoredFile(name = binary.name, isMain = true, contentType = binary.mimeType)
    }
    case CreateHtmlMaterial(_, _, main, _) => {
      VirtualFile(name = main.name, isMain = true, contentType = main.mimeType, content = main.content)
    }
  }

  def materialToResource[F <: File](sm: CreateNewMaterialRequest[F]): Resource = Resource(
    name = sm.name,
    materialType = Some(sm.materialType),
    files = Seq(toFile(sm)))
}

trait ItemSupportingMaterialHooks extends SupportingMaterialHooks with LoadOrgAndOptions with MaterialToResource {

  def auth: ItemAuth[OrgAndOpts]

  def itemService: ItemService

  def assets: ItemAssets

  override def create[F <: File](id: String, sm: CreateNewMaterialRequest[F])(implicit h: RequestHeader): R[JsValue] = {
    for {
      identity <- getOrgAndOptions(h)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      canWrite <- auth.canWrite(id)(identity)
    } yield {

      sm match {
        case CreateBinaryMaterial(name, materialType, binary) => {
          assets.upload(id, name, binary.name, binary.data)
          itemService.addSupportingMaterialResource(resource)
        }
        case CreateHtmlMaterial() =>
      }

    }
  }

  override def deleteAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def addAsset(id: String, name: String, binary: Binary)(implicit h: RequestHeader): R[JsValue] = ???

  override def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): SimpleResult = ???

  override def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue] = ???
}
