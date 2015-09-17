package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import play.api.Logger
import play.api.http.Writeable
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiDeleteIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  lazy val contentCollectionService = bootstrap.Main.contentCollectionService

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

      s"return $BAD_REQUEST - for bad itemId" in new orgWithAccessTokenAndItem() {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest("who's bad?", s"?access_token=$accessToken")
        assertStatus(r, BAD_REQUEST)
      }

      s"work" in new orgWithAccessTokenAndItem() {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest(itemId.toString, s"?access_token=$accessToken")
        route(r).map { result =>
          status(result) === OK
          val item = ItemHelper.get(itemId).get
          item.collectionId === contentCollectionService.archiveCollectionId.toString
        }.getOrElse(failure("no route found"))
      }

    }
  }
}
