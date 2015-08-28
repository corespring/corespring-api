package org.corespring.platform.core.models.item.resource

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification

class FileTest extends Specification {

  "StoredFile" should {

    "return a storage key for id with a version" in {
      val id = VersionedId(ObjectId.get, Some(0))
      val resource = Resource(name = "resource", files = Seq())
      val file = StoredFile("img.png", "image/png")
      StoredFile.storageKey(id.id, id.version.get, resource, file.name) === Seq(id.id, 0, resource.name, file.name).mkString("/")
    }
  }

}
