package org.corespring.platform.core.models.versioning

import VersionedIdImplicits.{ Writes, Reads }
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsSuccess, JsString }
import org.corespring.common.log.PackageLogging

class VersionedIdImplicitsTest extends Specification with PackageLogging {

  import VersionedIdImplicits.Binders._

  //TODO: SalatVersioningDao - migrate to 2.10
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
      Reads.reads(JsString("000000000000000000000001:0")) match {
        case JsSuccess(v, _) => {
          v.id.toString === "000000000000000000000001"
          v.version === Some(0)
          //v === VersionedId("000000000000000000000001", Some(0))
        }
        case _ => failure("reads failed")
      }
    }
  }

  "Writes" should {
    "write json" in {
      Writes.writes(VersionedId(new ObjectId("000000000000000000000001"), Some(0))) === JsString("000000000000000000000001:0")
    }
  }

}
