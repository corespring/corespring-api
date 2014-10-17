package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.scopes._
import org.specs2.specification.BeforeAfter
import play.api.mvc.Call

class V1ItemApiProxyTest extends IntegrationSpecification {
  val Routes = org.corespring.v2.api.routes.V1ItemApiProxy

  "V2 - V1ItemApiProxy" should {

    "get" should {

      s"return $OK" in new withItem {
        override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.get(itemId)

        status(result) === OK
      }
    }

    "list" should {

      s"return $OK" in new withItem {
        override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.list()

        status(result) === OK
      }
    }

    "listWithColl" should {

      s"return $OK" in new withItem {
        override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.listWithColl(collectionId)

        status(result) === OK
      }
    }

  }

  abstract class withItem extends BeforeAfter with itemLoader with TokenRequestBuilder with orgWithAccessTokenAndItem {

  }

}
