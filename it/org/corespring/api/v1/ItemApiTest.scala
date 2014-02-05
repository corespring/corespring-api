package org.corespring.api.v1

import org.corespring.it.{ ServerSpec, IntegrationSpecification }
import org.corespring.test.Assertions
import org.corespring.test.helpers.FixtureData
import play.api.libs.json.JsValue
import play.api.test.{ PlaySpecification, FakeRequest }
import play.api.mvc.Results
import org.specs2.specification.{ Step, Fragments }

class ItemApiTest //extends IntegrationSpecification {
  extends PlaySpecification with Results with ServerSpec {

  sequential

  override def map(fs: => Fragments) = {
    Step(println("-------------------> begin to start server")) ^
      Step(server.start()) ^
      Step(Thread.sleep(1000)) ^
      Step(println("-------------------> server started")) ^
      Step(Thread.sleep(1000)) ^
      fs ^
      Step(Thread.sleep(1000)) ^
      Step(println("-------------------> begin stopping server")) ^
      Step(Thread.sleep(1000)) ^
      Step(server.stop) ^
      Step(Thread.sleep(1000)) ^
      Step(println("-------------------> server stopped"))
  }

  /*"list items in a collection" in new FixtureData {
    println(s"[Test] collection: $collectionId, token: $accessToken")
    //TODO: Don't use magic strings for the routes - call the controller directly
    val fakeRequest = FakeRequest("", s"?access_token=$accessToken")
    val result = ItemApi.listWithColl(collectionId, None, None, "false", 0, 50, None)(fakeRequest)
    println(s" content: ---->  ${contentAsString(result)}")
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(itemIds.length)
  }*/
}
