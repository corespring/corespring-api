package tests.models.item.resource

import org.specs2.mutable.Specification
import models.item.resource.{Resource, StoredFile}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

class FileTest extends Specification{

  "StoredFile" should{
    "return a storage key for id with no version" in {
      val id = VersionedId(ObjectId.get)
      val resource = Resource("resource", Seq())
      val file = StoredFile("img.png", "image/png")
      StoredFile.storageKey(id, resource, file.name) === Seq(id.id, resource.name, file.name).mkString("/")
    }

    "return a storage key for id with a version" in {
      val id = VersionedId(ObjectId.get, Some(0))
      val resource = Resource("resource", Seq())
      val file = StoredFile("img.png", "image/png")
      StoredFile.storageKey(id, resource, file.name) === Seq(id.id, 0, resource.name, file.name).mkString("/")
    }
  }

}
