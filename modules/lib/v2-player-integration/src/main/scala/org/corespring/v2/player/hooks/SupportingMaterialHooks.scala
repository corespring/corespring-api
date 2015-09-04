package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.hooks.{ SupportingMaterialHooks => ContainerHooks, _ }
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.services.item.SupportingMaterialsService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Validation

trait SupportingMaterialHooks[ID]
  extends ContainerHooks
  with LoadOrgAndOptions
  with MaterialToResource
  with ContainerConverters {

  def auth: ItemAuth[OrgAndOpts]

  def service: SupportingMaterialsService[ID]

  implicit def ec: ExecutionContext

  def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, ID]

  private def executeWrite[A, RESULT](validationToResult: Validation[V2Error, A] => RESULT)(id: String, h: RequestHeader)(idToValidation: ID => Validation[String, A]): Future[RESULT] = Future {
    val out: Validation[V2Error, A] = for {
      identity <- getOrgAndOptions(h)
      vid <- parseId(id, identity)
      canWrite <- auth.canWrite(id)(identity)
      result <- idToValidation(vid).leftMap(e => generalError(e))
    } yield result

    validationToResult(out)
  }

  lazy val writeForResource = executeWrite[Resource, Either[(Int, String), JsValue]] { v =>
    v.map(r => Json.toJson(r))
  } _

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

  private def vToE[A](v: Validation[V2Error, A]) = {
    v.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue] = {

    def validate(v: Validation[V2Error, Seq[Resource]]) = {
      vToE(v.map(s => Json.toJson(s)))
    }

    val execute = executeWrite[Seq[Resource], Either[StatusMessage, JsValue]](validate) _
    execute(id, h) { vid =>
      service.delete(vid, name)
    }
  }

  override def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): Future[Either[StatusMessage, FileDataStream]] = {

    val execute = executeWrite[FileDataStream, Either[StatusMessage, FileDataStream]](vToE[FileDataStream]) _

    execute(id, h) { (vid) =>
      service.getFile(vid, name, filename).map { sfs =>
        FileDataStream(sfs.stream, sfs.contentLength, sfs.contentType, sfs.metadata)
      }
    }
  }

  override def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue] = writeForResource(id, h) { (vid) =>
    service.updateFileContent(vid, name, filename, content)
  }
}
