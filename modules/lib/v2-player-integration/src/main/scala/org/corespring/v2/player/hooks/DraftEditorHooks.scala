package org.corespring.v2.player.hooks

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, UploadResult }
import org.corespring.drafts.item.ItemDrafts
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile }
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

trait DraftEditorHooks extends ContainerEditorHooks with LoadOrgAndOptions with DraftHelper {

  import play.api.http.Status._

  import scalaz.Scalaz._
  import scalaz._

  private lazy val logger = V2LoggerFactory.getLogger("DraftEditorHooks")

  def transform: Item => JsValue

  def playS3: S3Service

  def bucket: String

  def backend: ItemDrafts

  def draftCollection = backend.collection

  private def getOrgAndUser(h: RequestHeader): Validation[V2Error, OrgAndUser] = getOrgAndOptions(h).map { oo =>
    OrgAndUser(SimpleOrg.fromOrganization(oo.org), oo.user.map(SimpleUser.fromUser))
  }

  private def loadDraft(id: String)(implicit header: RequestHeader): Validation[V2Error, ItemDraft] = for {
    _ <- Success(logger.trace(s"function=loadDraft id=$id"))
    identity <- getOrgAndUser(header)
    oid <- getOid(id, "load -> draftId")
    d <- backend.load(identity)(oid).toSuccess(generalError(s"Can't find draft with id: $id"))
  } yield d

  override def load(id: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"function=load id=$id")

    for {
      d <- loadDraft(id)
      item <- Success(d.src.data)
    } yield transform(item)
  }

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    logger.trace(s"function=loadFile id=$id path=$path")
    playS3.download(bucket, mkPath(id, path))
  }

  override def deleteFile(id: String, path: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"function=deleteFile id=$id path=$path")
    for {
      identity <- getOrgAndUser(header)
      oid <- getOid(id, "deleteFile -> draftId")
      owns <- Success(backend.owns(identity)(oid))
      _ <- if (owns) Success(true) else Failure(generalError(s"${identity.org.name}, can't access $id"))

    } yield {
      val response = playS3.delete(bucket, mkPath(id, path))
      if (response.success) {
        None
      } else {
        Some(BAD_REQUEST -> response.msg)
      }
    }
  }

  /**
   * Note: for now it is safe to assume that the asset will be put do and retrieved from the 'data' folder.
   * Because assets are disabled in supporting materials. When it comes to adding that back in we'll need to
   * change this. I'm proposing that the client uses the appropriate path, eg: data/img.png supporting-materials/name/img.png
   * @see PE-98
   * @param id
   * @param path
   * @return
   */
  def mkPath(id:String,path:String) = s"item-drafts/$id/data/$path"

  override def upload(id: String, path: String)(predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Future[UploadResult]] = {

    logger.trace(s"function=upload id=$id path=$path")

    def loadDraftPredicate(rh: RequestHeader): Either[SimpleResult, ItemDraft] = {
      predicate(rh).fold(
        loadDraft(id)(rh).leftMap { e => Results.Status(e.statusCode)(e.message) }.toEither)(Left(_))
    }

    def addFileToData(draft: ItemDraft, key: String) = {
      val filename = grizzled.file.util.basename(key)
      val newFile = StoredFile(path, BaseFile.getContentType(filename), false, filename)
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

    playS3.s3ObjectAndData[ItemDraft](bucket, mkPath(id, path))(loadDraftPredicate).map { f =>
      f.map { tuple =>
        val (s3Object, draft) = tuple
        addFileToData(draft, s3Object.getKey)
        UploadResult(s3Object.getKey)
      }
    }
  }

}
