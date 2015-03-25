package org.corespring.v2.player.hooks

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.hooks.{EditorHooks => ContainerEditorHooks, UploadResult}
import org.corespring.drafts.item.models.ItemDraft
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{Resource, BaseFile, StoredFile}
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{Auth, LoadOrgAndOptions}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

trait DraftEditorHooks extends ContainerEditorHooks with LoadOrgAndOptions {

  import play.api.http.Status._

  import scalaz._

  private lazy val logger = V2LoggerFactory.getLogger("EditorHooks")

  def draftCollection : MongoCollection

  def auth : Auth[ItemDraft,OrgAndOpts, ObjectId]

  def transform: Item => JsValue

  def playS3: S3Service

  def bucket: String

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future{
    val out : Validation[V2Error,OrgAndOpts] = getOrgAndOptions(header)//.toEither

    out.leftMap(e => e.statusCode -> e.message).flatMap{
      identity =>

        val result : Validation[V2Error, Item] = for{
          d <- auth.loadForWrite(id)(identity)
          item <- Success(d.src.data)
        } yield item

        def redirectIfReadable(e:V2Error) : (Int,String) = {
          logger.trace(s"can't load item: $id for writing - try to load for read and if successful return a SEE_OTHER")
          auth.loadForRead(id)(identity) match {
            case Success(draft) => {
              import org.corespring.container.client.controllers.apps.routes.Catalog
              SEE_OTHER -> Catalog.load(draft.src.data.id.toString).url
            }
            case Failure(e) => UNAUTHORIZED -> e.message
          }
        }
        result.bimap(redirectIfReadable, transform)
    }.toEither
  }


  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    playS3.download(bucket, mkPath(id, path))
  }

  override def deleteFile(id: String, file: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = {

    val out = for {
      identity <- getOrgAndOptions(header)
      canWrite <- auth.loadForWrite(id)(identity)
    } yield canWrite

    out match {
      case Failure(e) => Future(Some(UNAUTHORIZED -> e.message))
      case _ => {
        Future {
          val response = playS3.delete(bucket, mkPath(id, file))
          if (response.success) {
            None
          } else {
            Some(BAD_REQUEST -> response.msg)
          }
        }
      }
    }
  }

  def mkPath(parts: String*) = ("item-drafts" :+ parts).mkString("/")

  override def upload(draftId: String, file: String)(predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Future[UploadResult]] = {

    def loadDraft(rh: RequestHeader): Either[SimpleResult,ItemDraft] = {
      val result = for {
        identity <- getOrgAndOptions(rh)
        draft <- auth.loadForWrite(draftId)(identity)
      } yield draft
      result.leftMap{e => Results.Status(e.statusCode)(e.message)}.toEither
    }

    def addFileToData(draft:ItemDraft, key:String) = {
        val filename = grizzled.file.util.basename(key)
        val newFile = StoredFile(file, BaseFile.getContentType(filename), false, filename)
        import org.corespring.platform.core.models.mongoContext.context

        draft.src.data.data.map { d =>
          val dbo = com.novus.salat.grater[StoredFile].asDBObject(newFile)
          draftCollection.update(
            MongoDBObject("_id._id" -> draft.id),
            MongoDBObject("$addToSet" -> MongoDBObject("src.data.data.files" -> dbo)),
            false)
        }.getOrElse {

          val resource = Resource(None, "data", files = Seq(newFile))
          val resourceDbo = com.novus.salat.grater[Resource].asDBObject(resource)

          draftCollection.update(
            MongoDBObject("_id._id" -> draft.id),
            MongoDBObject("$set" -> MongoDBObject("src.data.data" -> resourceDbo)),
            false)
        }
    }

    playS3.s3ObjectAndData[ItemDraft](bucket, mkPath(draftId, file))(loadDraft).map { f =>
      f.map { tuple =>
        val (s3Object, draft) = tuple
        addFileToData(draft, s3Object.getKey)
        UploadResult(s3Object.getKey)
      }
    }
  }


}
