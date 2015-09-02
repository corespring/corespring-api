package org.corespring.platform.core.services.item

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.{ Context, grater }
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile, StoredFileDataStream }
import play.api.http.HeaderNames

import scala.util.Try
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

private[corespring] trait MainSupportingMaterialsService[A]
  extends SupportingMaterialsService[A] {

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

  private def materialNameEq(name: String) = materialsKey("name") $eq name

  private def fileNotPresent(name: String) = materialsKey("files.name") $ne name

  private def fileNameEq(name: String) = materialsKey("files.name") $ne name

  //override def create(vid: VersionedId[ObjectId], resource: Resource, bytes: => Array[Byte]): Validation[String, Resource] = {
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
      Failure("Update failed")
    }
  }

  //override def addFile(vid: VersionedId[ObjectId], materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource] = {
  override def addFile(id: A, materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource] = {
    val query = idToDbo(id) ++ fileNotPresent(file.name) ++ materialNameEq(materialName)
    val fileDbo = grater[BaseFile].asDBObject(file)
    val update = MongoDBObject("$push" -> MongoDBObject(prefix("supportingMaterials.$.files") -> fileDbo))
    val fields = MongoDBObject(prefix("supportingMaterials.$") -> 1)
    val maybeUpdated = collection.findAndModify(query, fields, sort = MongoDBObject.empty, remove = false, update, returnNew = true, upsert = false)

    maybeUpdated.map { dbo =>
      //Note: this expand may need tuning..
      val resourceDbo = dbo.expand[BasicDBObject]("supportingMaterials.0")
      val resource = grater[Resource].asObject(resourceDbo.get)
      file match {
        case sf: StoredFile => assets.upload(id, resource, sf, bytes).map(_ => resource)
        case _ => Success(resource)
      }
    }.getOrElse(Failure("Failed to update the document"))
  }

  //override def delete(vid: VersionedId[ObjectId], materialName: String): Validation[String, Seq[Resource]] = {
  override def delete(id: A, materialName: String): Validation[String, Seq[Resource]] = {
    val update = $pull(prefix("supportingMaterials") -> MongoDBObject("name" -> materialName))
    val query = idToDbo(id) ++ materialNameEq(materialName)

    for {
      preUpdateData <- findAndModify(query, update, returnNew = false).toSuccess("Can't update item")
      resources <- dbListToSeqResource(preUpdateData).leftMap(_.getMessage)
      resourceToDelete <- resources.find(_.name == materialName).toSuccess("Can't find resource that is to be deleted")
      remaining <- Success(resources.filterNot(_.name == materialName))
      hasStoredFiles <- Success(resourceToDelete.files.filter(isStoredFile).length > 0)
      assetDeletion <- if (hasStoredFiles) assets.deleteDir(id, resourceToDelete) else Success(true)
    } yield remaining
  }

  //override def updateFileContent(vid: VersionedId[ObjectId], materialName: String, file: String, content: String): Validation[String, Resource] = {
  override def updateFileContent(id: A, materialName: String, file: String, content: String): Validation[String, Resource] = {

    def getFiles(dbo: DBObject): Option[BasicDBList] = Try {
      val materials = getDbo(dbo, materialsKey().split("\\.").toList).asInstanceOf[BasicDBList]
      val m = materials.get(0).asInstanceOf[BasicDBObject]
      m.get("files").asInstanceOf[BasicDBList]
    }.toOption

    def updateFile(filename: String, content: String)(f: Any) = {
      val dbo = f.asInstanceOf[BasicDBObject]
      if (dbo.getString("name") == filename) {
        dbo.put("content", content)
      }
      dbo
    }

    val query = idToDbo(id) ++ materialNameEq(materialName) ++ fileNameEq(file)
    val fields = MongoDBObject(materialsKey("$") -> 1)

    def getUpdate(files: BasicDBList) = {
      val updatedFiles = files.map(updateFile(file, content))
      MongoDBObject("$set" -> MongoDBObject(materialsKey() -> updatedFiles))
    }

    for {
      dbo <- collection.findOne(query, fields).toSuccess(s"Can't find item with id: $id")
      files <- getFiles(dbo).toSuccess("can't load files from dbo")
      update <- Success(getUpdate(files))
      updateResult <- findAndModify(query, update, true, fields).toSuccess("Error updating")
      resources <- dbListToSeqResource(updateResult).leftMap(_.getMessage)
      head <- resources.headOption.toSuccess("Resource list is empty")
    } yield {
      head
    }
  }

  //override def removeFile(vid: VersionedId[ObjectId], materialName: String, filename: String): Validation[String, Resource] = {
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

  //override def getFile(vid: VersionedId[ObjectId], materialName: String, file: String, etag: Option[String]): Validation[String, StoredFileDataStream] = {
  override def getFile(id: A, materialName: String, file: String, etag: Option[String]): Validation[String, StoredFileDataStream] = {
    assets.getS3Object(id, materialName, file, etag).map { s3o =>
      val metadata = s3o.getObjectMetadata
      val fileMetadata = Map(HeaderNames.ETAG -> metadata.getETag)
      Success(StoredFileDataStream(file, s3o.getObjectContent, metadata.getContentLength, metadata.getContentType, fileMetadata))
    }.getOrElse(Failure("Can't find asset:"))
  }

  private def getDbo(dbo: DBObject, keys: List[String]): DBObject = {
    keys match {
      case Nil => dbo
      case head :: xs => {
        val inner = dbo.get(head).asInstanceOf[DBObject]
        getDbo(inner, xs)
      }
    }
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
