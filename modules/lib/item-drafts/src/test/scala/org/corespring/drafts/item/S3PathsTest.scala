package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification

class S3PathsTest extends Specification {

  val oid = ObjectId.get

  "S3Paths" should {

    "itemIdToPath" should {

      "replace colon with slash in full id" in {
        S3Paths.itemIdToPath(VersionedId(oid, Some(1))) === s"$oid/1"
      }

      "append default version 0 if no version in id" in {
        S3Paths.itemIdToPath(VersionedId(oid, None)) === s"$oid/0"
      }
    }
  }
}
