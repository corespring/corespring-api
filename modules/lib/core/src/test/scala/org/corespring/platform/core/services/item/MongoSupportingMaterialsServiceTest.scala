package org.corespring.platform.core.services.item

import com.amazonaws.services.s3.model.{ ObjectMetadata, S3Object, S3ObjectInputStream }
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource._
import org.corespring.platform.core.models.mongoContext
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.test.fakes.Fakes
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class MongoSupportingMaterialsServiceTest extends Specification with Mockito {

  PlaySingleton.start()

  import com.novus.salat.grater

  implicit def ctx: Context = mongoContext.context

  lazy val grt = grater[Resource]

  val virtualFile = VirtualFile("index.html", "text/html", false, "hi")
  val storedFile = StoredFile("image.png", "image/png", false)

  val defaultResource = Resource(name = "material", files = Seq(virtualFile, storedFile))

  class scope
    extends Scope
    with MongoSupportingMaterialsService[VersionedId[ObjectId]]
    with Fakes.withMockCollection
    with IdConverters {

    lazy val vid = new VersionedId(ObjectId.get, Some(0))

    val mockAssets = {
      val m = mock[SupportingMaterialsAssets[VersionedId[ObjectId]]]

      m.upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]]).answers { (args: Any, _) =>
        val array = args.asInstanceOf[Array[Any]]
        val file = array(2).asInstanceOf[StoredFile]
        Success(file)
      }

      m.deleteDir(any[VersionedId[ObjectId]], any[Resource]).answers { (args, _) =>
        val arr = args.asInstanceOf[Array[Any]]
        Success(arr(1).asInstanceOf[Resource])
      }

      m.getS3Object(any[VersionedId[ObjectId]], any[String], any[String], any[Option[String]]) returns {
        val s3o = mock[S3Object]
        s3o.getObjectMetadata returns {
          val m = mock[ObjectMetadata]
          m.getContentLength returns 0
          m.getContentType returns "image/png"
          m
        }
        s3o.getObjectContent returns mock[S3ObjectInputStream]
        Some(s3o)
      }

      m.deleteFile(any[VersionedId[ObjectId]], any[Resource], any[String]) returns {
        Success("ok")
      }
      m
    }

    override def idToDbo(id: VersionedId[ObjectId]): DBObject = vidToDbo(id)

    override def bucket: String = ""

    override def collection: MongoCollection = mockCollection

    override def assets: SupportingMaterialsAssets[VersionedId[ObjectId]] = mockAssets

    override implicit def ctx: Context = mongoContext.context
  }

  import org.corespring.platform.core.services.item.MongoSupportingMaterialsService.Errors._

  "create" should {

    val resourceWithStoredFile = defaultResource.copy(files = Seq(storedFile.copy(isMain = true)))

    class createScope(val resource: Resource = defaultResource) extends scope {
      val result = create(vid, resource, Array.empty)
    }

    "call collection.update" in new createScope() {
      val expectedQuery = ("supportingMaterials.name" $ne resource.name) ++ vidToDbo(vid)
      val expectedUpdate = $push("supportingMaterials" -> grt.asDBObject(resource))
      val (q, u) = captureUpdate
      q.value === expectedQuery
      u.value === expectedUpdate
    }

    "call assets.upload if the default file is a StoredFile" in new createScope(resourceWithStoredFile) {
      there was one(assets).upload(vid, resource, resource.defaultStoredFile.get, Array.empty)
    }

    "not call assets.upload if the default file is not a StoredFile" in new createScope {
      there was no(assets).upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]])
    }

    "return success if update succeeded" in new createScope() {
      result must_== Success(resource)
    }

    "return failure if update failed" in new scope() {
      override val updateResult = mockWriteResultWithN(0)
      val result = create(vid, defaultResource, Array.empty)
      result must_== Failure(updateFailed)
    }

    "return failure if upload failed" in new scope {
      assets.upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]]).returns(Failure("Upload failed"))
      val result = create(vid, resourceWithStoredFile, Array.empty)
      result must_== Failure("Upload failed")
    }
  }

  "addFile" should {

    class addFileScope[F <: BaseFile](val resource: Resource = defaultResource, val file: F = virtualFile) extends scope {
      val resourceDbo = grt.asDBObject(resource)
      override val findOneResult = MongoDBObject("supportingMaterials" -> MongoDBList(resourceDbo))
    }

    "when calling collection.findAndModify" should {

      "call collection.findOne with query" in new addFileScope {
        addFile(vid, defaultResource.name, file, Array.empty)
        val (query, _) = captureFindOne
        query.value === vidToDbo(vid) ++ ("supportingMaterials.name" $eq resource.name)
      }

      "call collection.findOne with fields" in new addFileScope {
        addFile(vid, defaultResource.name, file, Array.empty)
        val (_, fields) = captureFindOne
        fields.value === MongoDBObject("supportingMaterials" -> 1)
      }

      "returns file already exists error" in new addFileScope {
        val result = addFile(vid, defaultResource.name, file, Array.empty)
        result must_== Failure(fileAlreadyExists(file.name))
      }

      "call collection findAndModify" in new addFileScope(
        resource = defaultResource.copy(files = Seq.empty)) {
        addFile(vid, defaultResource.name, file, Array.empty)
        val (query, fields, update, _) = captureFindAndModify
        query.value === vidToDbo(vid) ++ ("supportingMaterials.name" $eq resource.name)
        update.value === $push("supportingMaterials.0.files" -> grater[BaseFile].asDBObject(file))
        fields.value === MongoDBObject("supportingMaterials" -> 1)
      }
    }

    "calls assets.upload if the file is a StoredFile" in new addFileScope(
      file = StoredFile("image.png", "image/png", false),
      resource = defaultResource.copy(files = Seq.empty)) {
      val updatedResource = resource.copy(files = resource.files :+ file)
      val mockUpdate = grt.asDBObject(updatedResource)
      override val findAndModifyResult = MongoDBObject("supportingMaterials" -> MongoDBList(mockUpdate))
      addFile(vid, defaultResource.name, file, Array.empty)
      there was one(assets).upload(vid, updatedResource, file, Array.empty)
    }

    "does not call assets.upload if the file is a StoredFile" in new addFileScope(
      resource = defaultResource.copy(files = Seq.empty)) {
      there was no(assets).upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]])
    }

    "returns the resource" in new addFileScope(
      resource = defaultResource.copy(files = Seq.empty)) {
      val updatedResource = resource.copy(files = resource.files :+ file)
      val mockUpdate = grt.asDBObject(updatedResource)
      override val findAndModifyResult = MongoDBObject("supportingMaterials" -> MongoDBList(mockUpdate))
      val result = addFile(vid, defaultResource.name, file, Array.empty)
      result must_== Success(updatedResource)
    }

    "returns a Failure if findAndModify returns nothing" in new scope {
      val result = addFile(vid, "name", StoredFile("image.png", "image/png", false), Array.empty)
      val query = idToDbo(vid) ++ materialNameEq("name")
      result must_== Failure(cantFindDocument(query))
    }
  }

  "delete" should {

    class deleteScope(val r: Resource = defaultResource, otherResources: Seq[Resource] = Seq.empty) extends scope {
      val allResources: Seq[DBObject] = (otherResources :+ r).map(grt.asDBObject)
      override val findAndModifyResult = MongoDBObject("supportingMaterials" -> MongoDBList(allResources: _*))
      val result = delete(vid, r.name)
    }

    "call collection.findAndModify with the correct update" in new deleteScope {
      val (_, _, u, _) = captureFindAndModify
      u.value === $pull("supportingMaterials" -> MongoDBObject("name" -> "material"))
    }

    "call collection.findAndModify with the correct query" in new deleteScope {
      val (q, _, _, _) = captureFindAndModify
      q.value === idToDbo(vid) ++ MongoDBObject("supportingMaterials.name" -> "material")
    }

    "calls assets.deleteDir if there are stored files" in new deleteScope(
      Resource(
        name = "material-with-assets",
        materialType = Some("Rubric"),
        files = Seq(
          StoredFile("image.png", "image/png", false)))) {
      there was one(assets).deleteDir(vid, r)
    }

    "not call assets.deleteDir if there are no stored files" in new deleteScope(
      defaultResource.copy(files = Seq(virtualFile))) {
      there was no(assets).deleteDir(any[VersionedId[ObjectId]], any[Resource])
    }

    "return the remaining resources" in new deleteScope {
      result must_== Success(Seq())
    }
  }

  "updateFileContent" should {

    val update = virtualFile.copy(content = "new content")
    val material = Resource(name = "my-material", files = Seq(virtualFile))
    val materials = Seq(material)
    val materialsDboList = MongoDBList(materials.map(grt.asDBObject): _*)

    "fail if document can't be found" in new scope {
      val result = updateFileContent(vid, "material", "filename", "hi")
      result must_== Failure(cantFindDocument(idToDbo(vid) ++ materialNameEq("material") ++ fileNameEq("filename")))
    }

    "fail if it cant load the files" in new scope {
      val dbo = MongoDBObject("supportingMaterials" -> MongoDBList.empty)
      override val findOneResult = dbo
      val result = updateFileContent(vid, "material", "filename", "hi")
      result must_== Failure(cantLoadFiles(dbo))
    }

    "calls collection.findAndModify with the update" in new scope {
      val dbo = MongoDBObject("supportingMaterials" -> materialsDboList)
      override val findOneResult = dbo
      val result = updateFileContent(vid, "material", "index.html", update.content)
      val (_, _, u, _) = captureFindAndModify
      u.value must_== $set("supportingMaterials.$.files" ->
        MongoDBList(
          grater[BaseFile].asDBObject(update)))
    }

    "returns the updated resource" in new scope {
      val dbo = MongoDBObject("supportingMaterials" -> materialsDboList)
      override val findOneResult = dbo
      override val findAndModifyResult = dbo
      val result = updateFileContent(vid, "material", "index.html", update.content)
      result must_== Success(Resource(name = "my-material", files = Seq(update)))
    }
  }

  "getFile" should {
    "call assets.getS3Object" in new scope {
      getFile(vid, "my-material", "file", None)
      there was one(assets).getS3Object(vid, "my-material", "file", None)
    }

    "returns a StoredFileDataStream" in new scope {
      val result = getFile(vid, "my-material", "file", None)
      result match {
        case Success(StoredFileDataStream(file, _, _, _, _)) => file must_== "file"
        case Failure(_) => failure
      }
    }
  }

  "removeFile" should {

    class removeFileScope(r: Resource = defaultResource, fileToDelete: String = virtualFile.name) extends scope {
      override val findAndModifyResult = MongoDBObject("supportingMaterials" -> MongoDBList(grt.asDBObject(r)))
      val result = removeFile(vid, r.name, fileToDelete)
    }

    "call collection.findAndModify with query" in new removeFileScope {
      val (q, _, _, _) = captureFindAndModify
      q.value === idToDbo(vid) ++ materialNameEq("material")
    }

    "call collection.findAndModify with update" in new removeFileScope {
      val (_, _, u, _) = captureFindAndModify
      u.value === $pull("supportingMaterials.$.files" -> MongoDBObject("name" -> virtualFile.name))
    }

    "not call assets.deleteFile" in new removeFileScope {
      there was no(assets).deleteFile(any[VersionedId[ObjectId]], any[Resource], any[String])
    }

    "call assets.deleteFile" in new removeFileScope(fileToDelete = storedFile.name) {
      there was one(assets).deleteFile(vid, defaultResource, storedFile.name)
    }

    "returns the updated resource" in new removeFileScope {
      result match {
        case Success(Resource(_, _, _, files)) => files.length must_== defaultResource.files.length - 1
        case Failure(_) => failure
      }
    }
  }
}
