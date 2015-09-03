package org.corespring.platform.core.services.item

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.{ Context, grater }
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile, StoredFileDataStream }
import org.corespring.platform.core.services.item.MongoSupportingMaterialsService.Errors
import play.api.Logger
import play.api.http.HeaderNames

import scala.util.Try
import scalaz.Scalaz._
import org.corespring.common.mongo.ExpandableDbo._
import scalaz.{ Failure, Success, Validation }

private[corespring] object MongoSupportingMaterialsService {
  object Errors {
    val updateFailed = "update failed"
    def cantFindDocument(q: DBObject) = s"Can't find a document with the query: ${q}"
    def cantFindResourceWithName(name: String, resources: Seq[Resource]) = s"Can't find a resource with name $name in names: ${resources.map(_.name)}"
    def cantLoadFiles(o: Any) = s"Can't load files from ${o}"
    val resourcesIsEmpty = "Resources list is empty"
  }
}

private[corespring] trait MongoSupportingMaterialsService[A]
  extends SupportingMaterialsService[A] {

  lazy val logger = Logger(classOf[MongoSupportingMaterialsService[A]])

  import Errors._

  def idToDbo(id: A): DBObject

  def collection: MongoCollection

  def bucket: String

  def assets: SupportingMaterialsAssets[A]

  def prefix(s: String): String = s

  implicit def ctx: Context

  private def materialsKey(key: String = "") = if (key.isEmpty) {
    prefix("supportingMaterials")
  } else {
    prefix(s"supportingMaterials.$key")
  }

  protected def materialNameEq(name: String) = materialsKey("name") $eq name

  protected def fileNotPresent(name: String) = materialsKey("files.name") $ne name

  protected def fileNameEq(name: String) = materialsKey("files.name") $ne name

  override def create(id: A, resource: Resource, bytes: => Array[Byte]): Validation[String, Resource] = {

    val nameNotPresent = prefix("supportingMaterials.name") $ne resource.name
    val query = nameNotPresent ++ idToDbo(id)
    val resourceDbo = grater[Resource].asDBObject(resource)
    val update = MongoDBObject("$push" -> MongoDBObject(prefix("supportingMaterials") -> resourceDbo))
    val result = collection.update(query, update, false, false)

    if (result.getN == 1) {
      resource.defaultStoredFile
        .map { sf =>
          assets.upload(id, resource, sf, bytes).map(_ => resource)
        }
        .getOrElse(Success(resource))
    } else {
      Failure(Errors.updateFailed)
    }
  }

  override def addFile(id: A, materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource] = {
    val query = idToDbo(id) ++ fileNotPresent(file.name) ++ materialNameEq(materialName)
    val fileDbo = grater[BaseFile].asDBObject(file)
    val update = MongoDBObject("$push" -> MongoDBObject(prefix("supportingMaterials.$.files") -> fileDbo))
    val fields = MongoDBObject(prefix("supportingMaterials.$") -> 1)
    val maybeUpdated = collection.findAndModify(query, fields, sort = MongoDBObject.empty, remove = false, update, returnNew = true, upsert = false)

    maybeUpdated.map { dbo =>

      val resourceDbo = dbo.expandPath("supportingMaterials.0")
        .getOrElse {
          throw new RuntimeException(s"Can't find supporting materials in db object: $dbo")
        }
      val resource = grater[Resource].asObject(resourceDbo)
      file match {
        case sf: StoredFile => assets.upload(id, resource, sf, bytes).map(_ => resource)
        case _ => Success(resource)
      }
    }.getOrElse(Failure(cantFindDocument(query)))
  }

  override def delete(id: A, materialName: String): Validation[String, Seq[Resource]] = {
    val update = $pull(prefix("supportingMaterials") -> MongoDBObject("name" -> materialName))
    val query = idToDbo(id) ++ materialNameEq(materialName)

    for {
      preUpdateData <- findAndModify(query, update, returnNew = false).toSuccess(cantFindDocument(query))
      resources <- dbListToSeqResource(preUpdateData).leftMap(_.getMessage)
      resourceToDelete <- resources.find(_.name == materialName).toSuccess(cantFindResourceWithName(materialName, resources))
      remaining <- Success(resources.filterNot(_.name == materialName))
      hasStoredFiles <- Success(resourceToDelete.files.filter(isStoredFile).length > 0)
      assetDeletion <- if (hasStoredFiles) assets.deleteDir(id, resourceToDelete) else Success(true)
    } yield remaining
  }

  override def updateFileContent(id: A, materialName: String, file: String, content: String): Validation[String, Resource] = {

    def getFiles(dbo: DBObject): Option[DBObject] = dbo.expandPath(materialsKey("0.files"))

    def updateFile(filename: String, content: String)(f: Any) = {
      val dbo = f.asInstanceOf[BasicDBObject]
      if (dbo.getString("name") == filename) {
        dbo.put("content", content)
      }
      dbo
    }

    val query = idToDbo(id) ++ materialNameEq(materialName) ++ fileNameEq(file)
    val fields = MongoDBObject(materialsKey("$") -> 1)

    def getUpdate(files: DBObject) = {
      val updatedFiles = files.map(updateFile(file, content))
      MongoDBObject("$set" -> MongoDBObject(materialsKey() -> updatedFiles))
    }

    for {
      dbo <- collection.findOne(query, fields).toSuccess(cantFindDocument(query))
      _ <- Success(println(s"dbo: $dbo"))
      files <- getFiles(dbo).toSuccess(cantLoadFiles(dbo))
      update <- Success(getUpdate(files))
      updateResult <- findAndModify(query, update, true, fields).toSuccess(cantFindDocument(query))
      resources <- dbListToSeqResource(updateResult).leftMap(_.getMessage)
      head <- resources.headOption.toSuccess(resourcesIsEmpty)
    } yield {
      head
    }
  }

  override def removeFile(id: A, materialName: String, filename: String): Validation[String, Resource] = {

    val query = idToDbo(id) ++ materialNameEq(materialName)
    val update = $pull(materialsKey("$.files") -> MongoDBObject("name" -> filename))

    for {
      update <- findAndModify(query, update, true).toSuccess("Update failed")
      resources <- dbListToSeqResource(update).leftMap(_.getMessage)
      resource <- resources.find(_.name == materialName).toSuccess("Can't find resource that was updated")
      assetDeletion <- assets.deleteFile(id, resource, filename).map(_ => resource)
    } yield resource
  }

  override def getFile(id: A, materialName: String, file: String, etag: Option[String]): Validation[String, StoredFileDataStream] = {
    assets.getS3Object(id, materialName, file, etag).map { s3o =>
      val metadata = s3o.getObjectMetadata
      val fileMetadata = Map(HeaderNames.ETAG -> metadata.getETag)
      Success(StoredFileDataStream(file, s3o.getObjectContent, metadata.getContentLength, metadata.getContentType, fileMetadata))
    }.getOrElse(Failure("Can't find asset:"))
  }

  private def toResource(dbo: DBObject) = grater[Resource].asObject(dbo)

  private def dbListToSeqResource(dbo: Any): Validation[Throwable, Seq[Resource]] = dbo match {
    case l: BasicDBList => {
      Validation.fromTryCatch {
        l.toArray.toSeq.map(o => toResource(o.asInstanceOf[BasicDBObject]))
      }
    }
    case _ => Failure(new IllegalArgumentException("Expected a BasicDBList"))
  }

  private def isStoredFile(file: BaseFile): Boolean = file match {
    case sf: StoredFile => true
    case _ => false
  }

  private lazy val returnMaterials = MongoDBObject(materialsKey() -> 1)

  private def findAndModify(query: DBObject, update: DBObject, returnNew: Boolean, fields: DBObject = returnMaterials) = {
    collection.findAndModify(query, fields, sort = MongoDBObject.empty, remove = false, update, returnNew, upsert = false)
  }
}
