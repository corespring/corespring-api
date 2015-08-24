package org.corespring.v2.player.hooks

import java.io.InputStream

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
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Success, Failure, Validation }

private[hooks] trait MaterialToResource {

  protected def binaryToFile(b: Binary, isMain: Boolean = false) = StoredFile(name = b.name, isMain = true, contentType = b.mimeType)
  protected def htmlToFile(h: Html, isMain: Boolean = false) = VirtualFile(name = h.name, isMain = true, contentType = h.mimeType, content = h.content)
  protected def requestToFile[F <: File](sm: CreateNewMaterialRequest[F]): BaseFile = sm match {
    case CreateBinaryMaterial(_, _, binary) => binaryToFile(binary)
    case CreateHtmlMaterial(_, _, main, _) => htmlToFile(main)
  }

  def materialToResource[F <: File](sm: CreateNewMaterialRequest[F]): Resource = Resource(
    name = sm.name,
    materialType = Some(sm.materialType),
    files = Seq(requestToFile(sm)))
}

case class FileData(stream: InputStream, contentLength: Int, contentType: String, metadata: Map[String, String])

trait SupportingMaterialsService {
  def create(vid: VersionedId[ObjectId], resource: Resource, bytes: => Array[Byte]): Validation[String, Resource]
  def delete(vid: VersionedId[ObjectId], materialName: String): Validation[String, Resource]
  def removeFile(vid: VersionedId[ObjectId], materialName: String, filename: String): Validation[String, Resource]
  def addFile(vid: VersionedId[ObjectId], materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource]
  def getFile(vid: VersionedId[ObjectId], materialName: String, file: String): Validation[String, FileData]
  def updateFileContent(vid: VersionedId[ObjectId], materialName: String, file: String, content: String): Validation[String, Resource]
}

trait ItemSupportingMaterialHooks
  extends SupportingMaterialHooks
  with LoadOrgAndOptions
  with MaterialToResource
  with ContainerConverters {

  def auth: ItemAuth[OrgAndOpts]

  def itemService: ItemService

  def service: SupportingMaterialsService

  private def executeWrite[A, RESULT](validationToResult: Validation[V2Error, A] => RESULT)(id: String, h: RequestHeader)(fn: VersionedId[ObjectId] => Validation[String, A]): Future[RESULT] = Future {
    val out: Validation[V2Error, A] = for {
      identity <- getOrgAndOptions(h)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      canWrite <- auth.canWrite(id)(identity)
      result <- fn(vid).leftMap(e => generalError(e))
    } yield result

    validationToResult(out)
  }

  lazy val writeForResource = executeWrite[Resource, Either[(Int, String), JsValue]](v => v.map(r => Json.toJson(r))) _

  override def create[F <: File](id: String, sm: CreateNewMaterialRequest[F])(implicit h: RequestHeader): R[JsValue] = {
    writeForResource(id, h) { (vid) =>
      val resource = materialToResource(sm)
      val bytes = sm match {
        case CreateBinaryMaterial(_, _, binary) => binary.data
        case _ => Array.empty[Byte]
      }
      service.create(vid, resource, bytes)
    }
  }

  override def deleteAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): R[JsValue] = writeForResource(id, h) { (vid) =>
    service.removeFile(vid, name, filename)
  }

  override def addAsset(id: String, name: String, binary: Binary)(implicit h: RequestHeader): R[JsValue] = writeForResource(id, h) { (vid) =>
    service.addFile(vid, name, binaryToFile(binary), binary.data)
  }

  override def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue] = writeForResource(id, h) { (vid) =>
    service.delete(vid, name)
  }

  override def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): Future[Either[StatusMessage, FileDataStream]] = {
    val execute = executeWrite[FileData, Either[StatusMessage, FileDataStream]] _
    execute { (v) =>
      v match {
        case Failure(e) => Left(e.statusCode, e.message) //play.api.mvc.Results.Status(e.statusCode)(e.message)
        case Success(fd) => {
          Right(fd)
        }
      }
    }(id, h) { (vid) =>
      service.getFile(vid, name, filename)
    }
  }

  override def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue] = writeForResource(id, h) { (vid) =>
    service.updateFileContent(vid, name, filename, content)
  }
}
