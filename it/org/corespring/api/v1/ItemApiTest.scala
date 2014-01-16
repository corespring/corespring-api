package org.corespring.api.v1

import org.corespring.it.ITSpec
import org.corespring.test.Assertions
import org.corespring.test.helpers.FixtureData
import play.api.libs.json.JsValue
import play.api.test.FakeRequest

class ItemApiTest extends ITSpec with Assertions{


  "list items in a collection" in new FixtureData {
    println(s"[Test] collection: $collectionId, token: $accessToken")
    //TODO: Don't use magic strings for the routes - call the controller directly
    val fakeRequest = FakeRequest("", s"?access_token=$accessToken")
    val result = ItemApi.listWithColl(collectionId, None, None, "false", 0, 50, None)(fakeRequest)
    println(s" content: ---->  ${contentAsString(result)}")
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(itemIds.length)
  }
}
