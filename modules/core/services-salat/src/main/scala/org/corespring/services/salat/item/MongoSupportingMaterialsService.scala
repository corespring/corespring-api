package org.corespring.platform.core.services.item

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.Logger
import com.novus.salat.{Context, grater}
import org.corespring.common.mongo.ExpandableDbo._
import org.corespring.models.item.resource.{StoredFileDataStream, BaseFile, Resource, StoredFile}
import org.corespring.platform.core.services.item.MongoSupportingMaterialsService.Errors
import org.corespring.services.item.SupportingMaterialsService

import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}

private[corespring] object MongoSupportingMaterialsService {
  object Errors {
    val updateFailed = "update failed"
    def cantFindDocument(q: DBObject) = s"Can't find a document with the query: $q"
    def cantFindResourceWithName(name: String, resources: Seq[Resource]) = s"Can't find a resource with name $name in names: ${resources.map(_.name)}"
    def cantLoadFiles(o: Any) = s"Can't load files from ${o}"
    def cantFindProperty(property: String, dbo: DBObject) = s"Can't find property: $property in $dbo"
    def cantFindAsset[A](id: A, material: String, file: String) = s"Can't find asset for $id, $material, $file"
    val resourcesIsEmpty = "Resources list is empty"
    def cantConvertToResources(dbo: DBObject) = s"Can't convert to a list of resources: $dbo"
    def cantFindResource(name: String, resources: Seq[Resource]) = s"Can't find '$name' in materials named: [${resources.map(_.name)}]"
    val cantLoadFirstResource = s"Can't load the first resource"
    def fileAlreadyExists(name: String) = s"This file already exists: $name"
  }
}

private[corespring] trait MongoSupportingMaterialsService[A]
  extends SupportingMaterialsService[A] {

  lazy val logger = Logger(classOf[MongoSupportingMaterialsService[A]])

  import Errors._

  val ETAG = "etag"

  def idToDbo(id: A): DBObject

  def collection: MongoCollection

  def bucket: String

  def assets: SupportingMaterialsAssets[A]

  def prefix(s: String): String = s

  logger.trace(s"prefix set to: [${prefix("")}]")

  implicit def ctx: Context

  private def materialsKey(key: String = "") = {
    val keyOut = if (key.isEmpty) {
      prefix("supportingMaterials")
    } else {
      prefix(s"supportingMaterials.$key")
    }
    keyOut
  }

  protected def materialNameEq(name: String) = materialsKey("name") $eq name

  protected def fileNotPresent(name: String) = materialsKey("files.name") $ne name

  protected def fileNameEq(name: String) = materialsKey("files.name") $eq name

  override def create(id: A, resource: Resource, bytes: => Array[Byte]): Validation[String, Resource] = {

    logger.debug(s"[create] id=$id, resource=${resource.name}")
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
    logger.debug(s"[addFile] id=$id, resource=${materialName}, file=${file.name}")
    val query = idToDbo(id) ++ materialNameEq(materialName)
    val fields = MongoDBObject(materialsKey() -> 1)

    def uploadFileIfNeeded(resource: Resource) = file match {
      case sf: StoredFile => assets.upload(id, resource, sf, bytes).map(_ => resource)
      case _ => Success(resource)
    }

    def addDataToMongoArray(index: Int) = {
      val fileDbo = grater[BaseFile].asDBObject(file)
      val update = $push(materialsKey(s"$index.files") -> fileDbo)
      findAndModify(query, update, true, MongoDBObject(materialsKey() -> 1))
    }

    for {
      dbo <- collection.findOne(query, fields).toSuccess(cantFindDocument(query))
      resources <- getResourcesFromDbo(dbo)
      resource <- resources.find(_.name == materialName).toSuccess(cantFindResource(materialName, resources))
      resourceIndex <- Success(resources.indexOf(resource))
      _ <- if (!resource.files.exists(_.name == file.name)) Success(true) else Failure(fileAlreadyExists(file.name))
      updatedDbo <- addDataToMongoArray(resourceIndex).toSuccess(cantFindDocument(query))
      updatedResources <- getResourcesFromDbo(updatedDbo)
      _ <- Success(logger.trace(s"[addFile] updatedResources: $updatedResources"))
      head <- Success(updatedResources(resourceIndex))
      _ <- uploadFileIfNeeded(head)
    } yield head
  }

  private def getResourcesFromDbo(dbo: DBObject): Validation[String, Seq[Resource]] = for {
    listDbo <- dbo.expandPath(materialsKey()).toSuccess(cantFindProperty(materialsKey(), dbo))
    resources <- dbListToSeqResource(listDbo).leftMap(_.getMessage)
  } yield resources

  override def delete(id: A, materialName: String): Validation[String, Seq[Resource]] = {
    logger.debug(s"[delete] id=$id, resource=${materialName}")
    val update = $pull(prefix("supportingMaterials") -> MongoDBObject("name" -> materialName))
    val query = idToDbo(id) ++ materialNameEq(materialName)

    for {
      preUpdateData <- findAndModify(query, update, returnNew = false).toSuccess(cantFindDocument(query))
      resources <- getResourcesFromDbo(preUpdateData)
      resourceToDelete <- resources.find(_.name == materialName).toSuccess(cantFindResourceWithName(materialName, resources))
      remaining <- Success(resources.filterNot(_.name == materialName))
      hasStoredFiles <- Success(resourceToDelete.files.filter(isStoredFile).length > 0)
      assetDeletion <- if (hasStoredFiles) assets.deleteDir(id, resourceToDelete) else Success(true)
    } yield remaining
  }

  override def updateFileContent(id: A, materialName: String, filename: String, content: String): Validation[String, Resource] = {
    logger.debug(s"[updateFileContent] id=$id, resource=${materialName}, file=$filename")

    def getFiles(dbo: DBObject): Option[MongoDBList] = dbo.expandPath(materialsKey("0.files"))
      .flatMap { r =>
        r match {
          case l: BasicDBList => Some(new MongoDBList(l))
          case _ => None
        }
      }

    def updateFile(filename: String, content: String)(dbo: DBObject) = {
      if (dbo.get("name") == filename) {
        dbo.put("content", content)
      }
      dbo
    }

    val query = idToDbo(id) ++ materialNameEq(materialName) ++ fileNameEq(filename)

    def getUpdate(files: MongoDBList) = {
      val updatedFiles = files.map(d => updateFile(filename, content)(d.asInstanceOf[DBObject]))
      MongoDBObject("$set" -> MongoDBObject(materialsKey("$.files") -> updatedFiles))
    }

    for {
      dbo <- collection.findOne(query, MongoDBObject(materialsKey("$") -> 1)).toSuccess(cantFindDocument(query))
      files <- getFiles(dbo).toSuccess(cantLoadFiles(dbo))
      update <- Success(getUpdate(files))
      updateResult <- findAndModify(query, update, true, MongoDBObject(materialsKey() -> 1)).toSuccess(cantFindDocument(query))
      _ <- Success(logger.trace(s"[updateFileContent] updateResult=$updateResult"))
      resources <- getResourcesFromDbo(updateResult)
      head <- resources.headOption.toSuccess(resourcesIsEmpty)
    } yield {
      head
    }
  }

  override def removeFile(id: A, materialName: String, filename: String): Validation[String, Resource] = {

    logger.debug(s"[removeFile] id=$id, resource=$materialName, file=$filename")

    def deleteAssetIfNecessary(r: Resource) = {
      val filtered = r.copy(files = r.files.filterNot(_.name == filename))
      if (r.files.exists(f => f.name == filename && f.isInstanceOf[StoredFile])) {
        logger.trace(s"[removeFile] id=$id, resource=$materialName, file=$filename - call assets.deletFile")
        assets.deleteFile(id, r, filename).map(_ => filtered)
      } else {
        Success(filtered)
      }
    }

    val query = idToDbo(id) ++ materialNameEq(materialName)
    val update = $pull(materialsKey("$.files") -> MongoDBObject("name" -> filename))

    logger.trace(s"[removeFile] id=$id, resource=$materialName, file=$filename, query=$query, update=$update")

    for {
      update <- findAndModify(query, update, false, fields = MongoDBObject(materialsKey() -> 1)).toSuccess("Update failed")
      _ <- Success(logger.trace(s"[removeFile] updateResult=$update"))
      resourceDbo <- update.expandPath(materialsKey("0")).toSuccess(cantFindProperty(materialsKey("0"), update))
      resource <- Success(grater[Resource].asObject(resourceDbo))
      filteredResource <- deleteAssetIfNecessary(resource)
    } yield filteredResource
  }

  override def getFile(id: A, materialName: String, filename: String, etag: Option[String]): Validation[String, StoredFileDataStream] = {
    logger.debug(s"[getFile] id=$id, resource=$materialName, file=$filename")
    assets.getS3Object(id, materialName, filename, etag).map { s3o =>
      val metadata = s3o.getObjectMetadata
      val fileMetadata = Map(ETAG -> metadata.getETag)
      Success(StoredFileDataStream(filename, s3o.getObjectContent, metadata.getContentLength, metadata.getContentType, fileMetadata))
    }.getOrElse(Failure(cantFindAsset(id, materialName, filename)))
  }

  private def toResource(dbo: DBObject) = {
    logger.trace(s"[toResource] dbo=$dbo")
    grater[Resource].asObject(dbo)
  }

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
