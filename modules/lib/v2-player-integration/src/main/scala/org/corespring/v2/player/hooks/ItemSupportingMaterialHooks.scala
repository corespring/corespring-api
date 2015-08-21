package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.{ StatusMessage, R }
import org.corespring.container.client.hooks._
import org.corespring.platform.core.models.item.resource.{ VirtualFile, BaseFile, StoredFile, Resource }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ generalError, cantParseItemId }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ SimpleResult, RequestHeader }
import play.api.http.Status._
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.Validation

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

  //def assets: ItemAssets

  implicit def validationToEither[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither

  protected def executeWrite(id: String, h: RequestHeader)(fn: VersionedId[ObjectId] => Validation[V2Error, JsValue]): R[JsValue] = Future {
    val result = for {
      identity <- getOrgAndOptions(h)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      canWrite <- auth.canWrite(id)(identity)
      json <- fn(vid)
    } yield json

    validationToEither[JsValue](result)
  }

  override def create[F <: File](id: String, sm: CreateNewMaterialRequest[F])(implicit h: RequestHeader): R[JsValue] = {
    executeWrite(id, h) { (vid) =>
      val resource = materialToResource(sm)
      val bytes = sm match {
        case CreateBinaryMaterial(_, _, binary) => binary.data
        case _ => Array.empty[Byte]
      }
      itemService.addSupportingMaterialResource(vid, resource, bytes).bimap(e => generalError(e), r => Json.toJson(r))
    }
  }

  override def deleteAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): R[JsValue] = {

  }

  override def addAsset(id: String, name: String, binary: Binary)(implicit h: RequestHeader): R[JsValue] = ???

  override def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): SimpleResult = ???

  override def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue] = ???
}
