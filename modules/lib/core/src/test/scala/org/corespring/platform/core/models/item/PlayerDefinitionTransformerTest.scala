package org.corespring.platform.core.models.item

import com.mongodb.DBObject
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import org.corespring.platform.core.models.item.resource.{ StoredFile, VirtualFile }
import org.corespring.platform.core.models.mongoContext
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

class PlayerDefinitionTransformerTest extends Specification {

  PlaySingleton.start

  "transformer" should {

    "serialize + deserialize with virtual files" in new serialize(
      new PlayerDefinition(
        Seq(VirtualFile("test.js", "text/javascript", false, "hello")),
        "",
        Json.obj()),
      MongoDBObject(
        "xhtml" -> "",
        "components" -> MongoDBObject(),
        "files" -> MongoDBList(
          MongoDBObject(
            "_t" -> "org.corespring.platform.core.models.item.resource.VirtualFile",
            "name" -> "test.js",
            "contentType" -> "text/javascript",
            "isMain" -> false,
            "content" -> "hello")))) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize with stored files" in new serialize(
      new PlayerDefinition(
        Seq(StoredFile("test.js", "text/javascript", false, "key")),
        "",
        Json.obj()),
      MongoDBObject(
        "xhtml" -> "",
        "components" -> MongoDBObject(),
        "files" -> MongoDBList(
          MongoDBObject(
            "_t" -> "org.corespring.platform.core.models.item.resource.StoredFile",
            "name" -> "test.js",
            "contentType" -> "text/javascript",
            "isMain" -> false,
            "storageKey" -> "key")))) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize json" in new serialize(
      new PlayerDefinition(Nil, "", Json.obj("2" -> Json.obj("name" -> "ed"))),
      MongoDBObject(
        "files" -> MongoDBList(),
        "xhtml" -> "",
        "components" -> MongoDBObject("2" -> MongoDBObject("name" -> "ed")))) {
      serialized === dbo
      deserialized === pd
    }

  }

  class serialize(val pd: PlayerDefinition, val dbo: DBObject) extends Scope {
    val serialized = new PlayerDefinitionTransformer(mongoContext.context).serialize(pd)
    val deserialized = new PlayerDefinitionTransformer(mongoContext.context).deserialize(dbo)
  }

}
