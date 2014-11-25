package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.itemSession._
import org.corespring.platform.core.services.item.ItemVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models._
import org.specs2.mutable._
import play.api.Play
import play.api.libs.json.Json
import se.radley.plugin.salat.SalatPlugin

class CanCastIntegerToLongTest extends IntegrationSpecification {

  private trait withAppContext extends After {
    val itemId = ObjectId.get
    val sessionItemJson = Json.obj(
      "itemId" -> Json.obj("_id" -> itemId.toString, "version" -> 0),
      "title" -> "some title",
      "question" -> "some question")

    val sessionService = new MongoService(db("itemsessions"))
    val sessionId = sessionService.create(sessionItemJson)

    def db = Play.current.plugin[SalatPlugin].get.db()

    def after = {
      println(s"[withV2Session] after")
      sessionService.delete(sessionId.get.toString)
    }
  }

  "This test" should {

    "not throw a ClassCastException after using the ItemApi" in new withAppContext {
      val sessionItem: ItemSession = DefaultItemSession.get(sessionId.get)(false).get
      val version = sessionItem.itemId.version
      version === Some(0)
      version.get.toLong === 0L
    }


  }
}