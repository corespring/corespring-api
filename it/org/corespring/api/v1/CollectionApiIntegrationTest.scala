package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.ContentCollection
import org.corespring.test.helpers.models._
import org.corespring.v2.player.scopes
import play.api.libs.json.{ Json, JsValue }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some

trait collectionData extends scopes.orgWithAccessToken {

  def count: Int = 1

  val collectionIds = CollectionHelper.createMultiple(orgId, count)

  def collection(id: ObjectId) = ContentCollection.findOneById(id).getOrElse(throw new RuntimeException(s"no coll with id $id"))

  override def before: Any = {
    super.before
  }

  override def after: Any = {
    super.after
    CollectionHelper.delete(collectionIds: _*)
  }

}

trait list extends collectionData {
  def url: String
  val fakeRequest = FakeRequest(GET, url)
  val Some(result) = route(fakeRequest)
  val jsonCollection = contentAsJson(result).as[Seq[JsValue]]
}

trait one extends collectionData {
  def url: String
  val fakeRequest = FakeRequest(GET, url)
  val Some(result) = route(fakeRequest)
  val json = contentAsJson(result)
}

class CollectionApiIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.api.v1.routes.CollectionApi

  "list all collections" in new list {
    override def url = s"${routes.list().url}?access_token=$accessToken"
    jsonCollection.size === 1
  }

  "list all collections skipping the 2 results" in new list {
    override def count = 5
    override def url = s"${routes.list(sk = 2).url}&access_token=$accessToken"
    jsonCollection.size === 3
  }

  "list all collections limit results to 1" in new list {
    override def count = 10
    override def url = s"${routes.list(l = 2)}&access_token=$accessToken"
    jsonCollection.size === 2
  }

  "find a collection with name matching name" in new list {

    override def url = {
      val name = collection(collectionIds(0)).name
      val query = Json.stringify(Json.obj("name" -> name))
      val u = routes.list(q = Some(query)).url
      s"$u&access_token=$accessToken"
    }
    jsonCollection.size === 1
    (jsonCollection(0) \ "name").as[String] === collection(collectionIds(0)).name
  }

  "find a collection by id" in new one {
    override def url = s"${routes.getCollection(collectionIds(0)).url}?access_token=$accessToken"
    (json \ "name").as[String] === collection(collectionIds(0)).name
  }
}
