package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.models.item.resource.{ StoredFile, VirtualFile }
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class FileTransformerTest extends Specification {

  trait scope extends Scope {
    lazy val virtualFileDbo = MongoDBObject("name" -> "test.css", "contentType" -> "text/css", "isMain" -> false, "content" -> "body{}")
    lazy val virtualFile = VirtualFile("test.css", "text/css", false, "body{}")

    lazy val storedFileDbo = MongoDBObject("name" -> "img.png", "contentType" -> "image/png", "isMain" -> false, "storageKey" -> "blah")
    lazy val storedFile = StoredFile("img.png", "image/png", false, "blah")

    val transformer = new FileTransformer()
  }

  "serialize" should {
    "serialize a VirtualFile" in new scope {
      transformer.serialize(virtualFile) === virtualFileDbo
    }

    "serialize a StoredFile" in new scope {
      transformer.serialize(storedFile) === storedFileDbo
    }
  }

  "deserialize" should {

    "deserialize a VirtualFile" in new scope {
      virtualFile === transformer.deserialize(virtualFileDbo)
    }

    "deserialize a StoredFile" in new scope {
      storedFile === transformer.deserialize(storedFileDbo)
    }
  }
}
