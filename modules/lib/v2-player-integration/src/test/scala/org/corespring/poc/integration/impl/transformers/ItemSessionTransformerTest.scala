package org.corespring.poc.integration.impl.transformers

import org.corespring.platform.core.models.itemSession.ItemSession
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.corespring.qti.models.responses.{ArrayResponse, StringResponse}
import org.corespring.test.utils.JsonCompare
import play.api.libs.json.Json
import play.api.Logger

class ItemSessionTransformerTest extends Specification{

  private val logger = Logger("poc")

  "ItemSessionTransformer" should {

    "transform to a v2 session and back again" in {

      val oid = ObjectId.get

      val session = ItemSession(
        id = oid,
        itemId = VersionedId(oid),
        responses = Seq(
          StringResponse(id="1", responseValue = "1"),
          ArrayResponse(id="2", responseValue = Seq("2.1", "2.2"))
        )
      )
      val out = ItemSessionTransformer.toV2Session(session)

      val expectedJson =
        s"""
          {
            "id" : "$oid",
            "itemId" : "$oid",
            "components" : {
              "1" : {
                "answers" : "1"
               },
              "2" : {
                "answers" : ["2.1", "2.2"]
              }
            }
          }
        """.stripMargin

      JsonCompare.caseInsensitiveSubTree(Json.parse(expectedJson), out) match {
        case Left(diffs) => {
          println(diffs.mkString("\n"))
          logger.debug(diffs.mkString("\n"))
          failure("not equal")
        }
        case Right(_) => success
      }

      val returnedSession = ItemSessionTransformer.toItemSession(Json.parse(expectedJson))

      returnedSession === session
    }
  }

}
