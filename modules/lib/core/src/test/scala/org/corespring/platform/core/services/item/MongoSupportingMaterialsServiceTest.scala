package org.corespring.platform.core.services.item

import com.mongodb.{ WriteResult, DBObject }
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.{ BaseFile, VirtualFile, StoredFile, Resource }
import org.corespring.platform.core.models.mongoContext
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.test.fakes.Fakes
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class MongoSupportingMaterialsServiceTest extends Specification with Mockito {

  PlaySingleton.start()

  import com.novus.salat.grater

  class scope
    extends Scope
    with MongoSupportingMaterialsService[VersionedId[ObjectId]]
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
      m
    }

    val mockCollection = {
      val fake = new Fakes.MongoCollection(1)
      fake
    }

    override def idToDbo(id: VersionedId[ObjectId]): DBObject = vidToDbo(id)

    override def bucket: String = ""

    override def collection: MongoCollection = mockCollection

    override def assets: SupportingMaterialsAssets[VersionedId[ObjectId]] = mockAssets

    override implicit def ctx: Context = mongoContext.context
  }

  import org.corespring.platform.core.services.item.MongoSupportingMaterialsService.Errors._

  "create" should {

    val defaultResource = Resource(name = "new material", files = Seq(
      StoredFile("img.png", "image/png", true)))

    val noFilesResource = Resource(name = "new material", files = Seq.empty)

    class createScope(val resource: Resource = defaultResource) extends scope {
      val result = create(vid, resource, Array.empty)
    }

    "call collection.update" in new createScope() {
      val expectedQuery = ("supportingMaterials.name" $ne "new material") ++ vidToDbo(vid)
      val expectedUpdate = $push("supportingMaterials" -> grater[Resource].asDBObject(resource))
      expectedQuery === mockCollection.queryObj
      expectedUpdate === mockCollection.updateObj
    }

    "call assets.upload if the default file is a StoredFile" in new createScope() {
      there was one(assets).upload(vid, resource, resource.defaultStoredFile.get, Array.empty)
    }

    "not call assets.upload if the default file is not a StoredFile" in new createScope(noFilesResource) {
      there was no(assets).upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]])
    }

    "return success if update succeeded" in new createScope() {
      result must_== Success(resource)
    }

    "return failure if update failed" in new scope() {
      mockCollection.n = 0
      val result = create(vid, defaultResource, Array.empty)
      result must_== Failure(updateFailed)
    }

    "return failure if upload failed" in new scope() {
      assets.upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]]).returns(Failure("Upload failed"))
      val result = create(vid, defaultResource, Array.empty)
      result must_== Failure("Upload failed")
    }
  }

  "addFile" should {

    val htmlFile = VirtualFile("index.html", "text/html", false, "hi")

    class addFileScope[F <: BaseFile](val file: F = htmlFile) extends scope {
      val fakeResource = Resource(name = "fake result", files = Seq(file))
      val dbResult = MongoDBObject("supportingMaterials" -> MongoDBList(grater[Resource].asDBObject(fakeResource)))
      mockCollection.findAndModifyResult = Some(dbResult)
      val result = addFile(vid, "name", file, Array.empty)
    }

    "when calling collection.findAndModify" should {

      "call collection.findAndModify with query" in new addFileScope {
        mockCollection.queryObj === vidToDbo(vid) ++
          ("supportingMaterials.files.name" $ne "index.html") ++
          ("supportingMaterials.name" $eq "name")
      }

      "call collection.findAndModify with update" in new addFileScope {
        mockCollection.updateObj === $push("supportingMaterials.$.files" -> grater[BaseFile].asDBObject(file))
      }

      "call collection.findAndModify with fields" in new addFileScope {
        mockCollection.fieldsObj === MongoDBObject("supportingMaterials.$" -> 1)
      }
    }

    "calls assets.upload if the file is a StoredFile" in new addFileScope(
      StoredFile("image.png", "image/png", false)) {
      there was one(assets).upload(vid, fakeResource, file, Array.empty)
    }

    "calls assets.upload if the file is a StoredFile" in new addFileScope {
      there was no(assets).upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]])
    }

    "returns the resource" in new addFileScope {
      result must_== Success(fakeResource)
    }

    "returns a Failure if findAndModify returns nothing" in new scope {
      mockCollection.findAndModifyResult = None
      val result = addFile(vid, "name", StoredFile("image.png", "image/png", false), Array.empty)
      val query = idToDbo(vid) ++ fileNotPresent("image.png") ++ materialNameEq("name")
      result must_== Failure(cantFindDocument(query))
    }
  }

  "delete" should {

    val defaultResource = Resource(name = "material", materialType = Some("Rubric"), files = Seq.empty)

    class deleteScope(val r: Resource = defaultResource, otherResources: Seq[Resource] = Seq.empty) extends scope {
      val allResources: Seq[DBObject] = (otherResources :+ r).map(grater[Resource].asDBObject)
      mockCollection.findAndModifyResult = Some(MongoDBList(allResources: _*))
      val result = delete(vid, r.name)
    }

    "call collection.findAndModify with the correct update" in new deleteScope {
      mockCollection.updateObj === $pull("supportingMaterials" -> MongoDBObject("name" -> "material"))
    }

    "call collection.findAndModify with the correct query" in new deleteScope {
      mockCollection.queryObj === idToDbo(vid) ++ MongoDBObject("supportingMaterials.name" -> "material")
    }

    "calls assets.deleteDir if there are stored files" in new deleteScope(
      Resource(
        name = "material-with-assets",
        materialType = Some("Rubric"),
        files = Seq(
          StoredFile("image.png", "image/png", false)))) {
      there was one(assets).deleteDir(vid, r)
    }

    "not call assets.deleteDir if there are no stored files" in new deleteScope {
      there was no(assets).deleteDir(any[VersionedId[ObjectId]], any[Resource])
    }

    "return the remaining resources" in new deleteScope {
      result must_== Success(Seq())
    }
  }

  "updateFileContent" should {
    //    class updateFileContentScope extends scope {
    //      mockCollection.findOneResult = None
    //
    //    }

    "fail if document can't be found" in new scope {
      mockCollection.findOneResult = None
      val result = updateFileContent(vid, "material", "filename", "hi")
      result must_== Failure(cantFindDocument(idToDbo(vid) ++ materialNameEq("material") ++ fileNameEq("filename")))
    }

    "fail if it cant load the files" in new scope {
      val dbo = MongoDBObject("supportingMaterials" -> MongoDBList.empty)
      mockCollection.findOneResult = Some(dbo)
      val result = updateFileContent(vid, "material", "filename", "hi")
      result must_== Failure(cantLoadFiles(dbo))
    }

    "calls collection.findAndModify with the update" in new scope {
      val htmlFile = VirtualFile("index.html", "text/html", true, "old content")
      val update = htmlFile.copy(content = "new content")
      val files = MongoDBList(
        grater[BaseFile].asDBObject(htmlFile))
      val materials = MongoDBList(
        MongoDBObject("files" -> files))
      val dbo = MongoDBObject("supportingMaterials" -> materials)
      mockCollection.findOneResult = Some(dbo)
      val result = updateFileContent(vid, "material", "index.html", update.content)
      mockCollection.updateObj must_== $set("supportingMaterials.$.files" ->
        MongoDBList(
          grater[BaseFile].asDBObject(update)))
    }
  }

  "getFile" should {
    "work" in pending
  }

  "removeFile" should {
    "work" in pending
  }
}
