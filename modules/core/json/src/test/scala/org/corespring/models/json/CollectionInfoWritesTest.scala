package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollection }
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class CollectionInfoWritesTest extends Specification {

  "writes" should {

    "write json" in {

      val orgId = ObjectId.get
      val id = ObjectId.get
      CollectionInfoWrites.writes(CollectionInfo(ContentCollection(
        "name",
        orgId,
        false,
        id), 10, orgId, Permission.Read)) must_== Json.parse(
        s"""
           |{"itemCount":10,
           |"permission":"${Permission.Read.name}",
           |"name":"name",
           |"ownerOrgId":"$orgId",
           |"isPublic":false,
           |"id":"$id"}""".stripMargin)
    }
  }
}
