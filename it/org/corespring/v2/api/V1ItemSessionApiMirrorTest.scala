package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ Resource, VirtualFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.{ CollectionHelper, ItemHelper }
import org.corespring.platform.core.models.item._
import org.corespring.v2.player.scopes._
import org.specs2.specification.BeforeAfter
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, AnyContentAsJson, Call }
import play.api.test.{ FakeHeaders, FakeRequest }


class V1ItemSessionApiMirrorTest extends IntegrationSpecification {
  val Routes = org.corespring.v2.api.routes.V1ItemSessionApiMirror

  "V2 - V1ItemSessionApiMirror" should {

    "reopen" should {

      s"return $OK" in new withItemAndSession {
        status(result) === OK
      }
    }
  }

  class withItemAndSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndV1Session {
    override def getCall(sessionId: ObjectId): Call = Routes.reopen(itemId, sessionId)
  }

}
