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

class MainSupportingMaterialsServiceTest extends Specification with Mockito {

  PlaySingleton.start()

  import com.novus.salat.grater

  class scope
    extends Scope
    with MainSupportingMaterialsService[VersionedId[ObjectId]]
    with IdConverters {

    lazy val vid = new VersionedId(ObjectId.get, Some(0))

    val mockAssets = {
      val m = mock[SupportingMaterialsAssets[VersionedId[ObjectId]]]

      m.upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]]).answers { (args: Any, _) =>
        val array = args.asInstanceOf[Array[Any]]
        val file = array(2).asInstanceOf[StoredFile]
        Success(file)
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
      result must_== Failure("Update failed")
    }

    "return failure if upload failed" in new scope() {
      assets.upload(any[VersionedId[ObjectId]], any[Resource], any[StoredFile], any[Array[Byte]]).returns(Failure("Upload failed"))
      val result = create(vid, defaultResource, Array.empty)
      result must_== Failure("Upload failed")
    }
  }

  "addFile" should {

    "when calling collection.findAndModify" should {

      class addFileScope extends scope {
        val file = VirtualFile("index.html", "text/html", false, "hi")
        addFile(vid, "name", file, Array.empty)
      }

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

    "calls assets.upload if the file is a StoredFile" in new scope() {
      val file = StoredFile("index.html", "text/html", false)
      val fakeResource = Resource(name = "fake result", files = Seq(file))
      val dbResult = MongoDBObject("supportingMaterials" -> MongoDBList(grater[Resource].asDBObject(fakeResource)))
      mockCollection.findAndModifyResult = Some(dbResult)
      addFile(vid, "name", file, Array.empty)
      there was one(assets).upload(vid, fakeResource, file, Array.empty)
    }
  }

  "delete" should {
    "work" in pending
  }

  "updateFileContent" should {
    "work" in pending
  }

  "getFile" should {
    "work" in pending
  }

  "removeFile" should {
    "work" in pending
  }
}
