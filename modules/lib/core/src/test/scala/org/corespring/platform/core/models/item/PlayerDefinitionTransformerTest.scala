package org.corespring.platform.core.models.item

import com.mongodb.{ BasicDBList, DBObject }
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import org.corespring.models.item.resource.{ StoredFile, VirtualFile }
import org.corespring.models.mongoContext
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

class PlayerDefinitionTransformerTest extends Specification {

  PlaySingleton.start

  implicit def unwrapDBList(in: MongoDBList): BasicDBList = in.underlying

  val files: BasicDBList = MongoDBList(MongoDBObject())

  "transformer" should {

    "serialize + deserialize with virtual files" in new serialize(

      new PlayerDefinition(
        Seq(VirtualFile("test.js", "text/javascript", false, "hello")),
        "",
        Json.obj(),
        "", None),
      MongoDBObject(
        "xhtml" -> "",
        "summaryFeedback" -> "",
        "components" -> MongoDBObject(),
        "files" -> unwrapDBList(MongoDBList(
          MongoDBObject(
            "_t" -> "org.corespring.platform.core.models.item.resource.VirtualFile",
            "name" -> "test.js",
            "contentType" -> "text/javascript",
            "isMain" -> false,
            "content" -> "hello"))))) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize with stored files" in new serialize(
      new PlayerDefinition(
        Seq(StoredFile("test.js", "text/javascript", false, "key")),
        "",
        Json.obj(),
        "", Some("function(){}")),
      MongoDBObject(
        "xhtml" -> "",
        "summaryFeedback" -> "",
        "components" -> MongoDBObject(),
        "customScoring" -> "function(){}",
        "files" -> unwrapDBList(MongoDBList(
          MongoDBObject(
            "_t" -> "org.corespring.platform.core.models.item.resource.StoredFile",
            "name" -> "test.js",
            "contentType" -> "text/javascript",
            "isMain" -> false,
            "storageKey" -> "key"))))) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize json" in new serialize(
      new PlayerDefinition(Nil, "", Json.obj("2" -> Json.obj("name" -> "ed")), "", None),
      MongoDBObject(
        "files" -> unwrapDBList(MongoDBList()),
        "xhtml" -> "",
        "components" -> MongoDBObject("2" -> MongoDBObject("name" -> "ed")),
        "summaryFeedback" -> "")) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize xhtml" in new serialize(
      new PlayerDefinition(Nil, "<div>something</div>", Json.obj(), "", None),
      MongoDBObject(
        "files" -> unwrapDBList(MongoDBList()),
        "xhtml" -> "<div>something</div>",
        "components" -> MongoDBObject(),
        "summaryFeedback" -> "")) {
      serialized === dbo
      deserialized === pd
    }

    "serialize + deserialize summaryFeedback" in new serialize(
      new PlayerDefinition(Nil, "", Json.obj(), "The show ended with a riot of feedbacking guitars.", None),
      MongoDBObject(
        "files" -> unwrapDBList(MongoDBList()),
        "xhtml" -> "",
        "components" -> MongoDBObject(),
        "summaryFeedback" -> "The show ended with a riot of feedbacking guitars.")) {
      serialized === dbo
      deserialized === pd
    }

    "with an empty dbo" should {
      val dbo = MongoDBObject()
      val pd = new PlayerDefinitionTransformer(mongoContext.context).deserialize(dbo)

      "set summaryFeedback to an empty string" in {
        pd.summaryFeedback === ""
      }

      "set customScoring to None" in {
        pd.customScoring === None
      }

      "set files to empty list" in {
        pd.files === Seq.empty
      }

      "set xhtml to an empty string" in {
        pd.xhtml === ""
      }

      "set json to an empty object" in {
        pd.components === Json.obj()
      }

    }

  }

  class serialize(val pd: PlayerDefinition, val dbo: DBObject) extends Scope {
    val serialized = new PlayerDefinitionTransformer(mongoContext.context).serialize(pd)
    val deserialized = new PlayerDefinitionTransformer(mongoContext.context).deserialize(dbo)
  }

}
