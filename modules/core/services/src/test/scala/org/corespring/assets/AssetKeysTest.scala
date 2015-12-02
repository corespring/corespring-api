package org.corespring.assets

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification

class AssetKeysTest extends Specification {

  val keys = new AssetKeys[String] {
    override def folder(id: String): String = return id
  }

  "folder" should {
    "return the key" in {
      keys.file("dir", "file") must_== "dir/data/file"
    }
  }

  "supportingMaterialFolder" should {
    "return the correct key" in {
      keys.supportingMaterialFolder("dir", "material") must_== "dir/materials/material"
    }
  }

  "supportingMaterialFile" should {
    "return the correct key" in {
      keys.supportingMaterialFile("dir", "material", "file") must_== "dir/materials/material/file"
    }
  }
}

class ItemAssetKeysTest extends Specification {

  val oid = ObjectId.get

  "folder" should {

    "throw an exception if the version is empty" in {
      ItemAssetKeys.folder(new VersionedId(oid)) must throwA[RuntimeException]
    }

    "return the folder key" in {
      ItemAssetKeys.folder(new VersionedId(oid, Some(0))) must_== s"$oid/0"
    }

  }
}
