package tests.models.versioning

import org.specs2.mutable.Specification
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import models.versioning.VersionedIdImplicits.{Writes, Reads}
import play.api.libs.json.JsString

class VersionedIdImplicitsTest extends Specification{

  import models.versioning.VersionedIdImplicits.Binders._

  "Binder" should {

    "convert string to versioned id" in {
      stringToVersionedId("000000000000000001") === None
      stringToVersionedId("000000000000000000000001") === Some(VersionedId(new ObjectId("000000000000000000000001"), None))
      stringToVersionedId("000000000000000000000001:0") === Some(VersionedId(new ObjectId("000000000000000000000001"), Some(0)))
      stringToVersionedId("000000000000000000000001:apple") === Some(VersionedId(new ObjectId("000000000000000000000001"), None))
    }

    "convert versioned id to string" in {
      versionedIdToString(VersionedId(new ObjectId("000000000000000000000001"), None)) === "000000000000000000000001"
      versionedIdToString(VersionedId(new ObjectId("000000000000000000000001"), Some(1))) === "000000000000000000000001:1"
      versionedIdToString(VersionedId(new ObjectId("000000000000000000000001"), Some(3))) === "000000000000000000000001:3"
    }
  }

  "Reads" should {
    "read json" in {
      Reads.reads(JsString("000000000000000000000001:0")) === VersionedId("000000000000000000000001", Some(0))
    }
  }

  "Writes" should {
    "write json" in {
      Writes.writes(VersionedId( new ObjectId("000000000000000000000001"), Some(0))) === JsString("000000000000000000000001:0")
    }
  }

}
