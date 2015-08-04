package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.resource.{Resource, VirtualFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models.{CollectionHelper, ItemHelper}
import org.corespring.v2.player.scopes.{orgWithAccessTokenAndItem, orgWithAccessToken}
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}

class ItemApiDeleteIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "V2 - ItemApi" should {
    "delete" should {

      val logger = Logger("it")

      def assertStatus[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]) = {
        route(r).map { result =>
          status(result) === expectedStatus
        }.getOrElse(failure("no route found"))
      }

      def createRequest(itemId: String, query: String = "") = {
        FakeRequest(
          routes.delete(itemId).method,
          s"${routes.delete(itemId).url}$query",
          FakeHeaders(),
          AnyContentAsEmpty)
      }

      s"return $BAD_REQUEST - for bad itemId" in new orgWithAccessTokenAndItem(){
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest("who's bad?", s"?access_token=$accessToken")
        assertStatus(r, BAD_REQUEST)
      }

      s"work" in new orgWithAccessTokenAndItem(){
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest(itemId.toString, s"?access_token=$accessToken")
        route(r).map { result =>
          status(result) === OK
          val item = ItemHelper.get(itemId).get
          item.collectionId.get === ContentCollection.archiveCollId.toString
        }.getOrElse(failure("no route found"))
      }

    }
  }
}
