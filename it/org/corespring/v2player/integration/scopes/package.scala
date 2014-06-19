package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models._
import org.specs2.mutable.BeforeAfter
import play.api.Logger
import play.api.http.{ Writeable, ContentTypeOf }
import play.api.mvc.{ Cookie, Request, AnyContent, Call }
import play.api.test.FakeRequest

package object scopes {

  val logger = Logger("it.scopes")

  trait orgWithAccessToken extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val user = UserHelper.create(orgId, "test_user")
    val accessToken = AccessTokenHelper.create(orgId, "test_user")

    def before: Any = {
      logger.debug(s"[before] apiClient ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    }

    def after: Any = {
      logger.trace(s"[after] deleting db data: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
      ApiClientHelper.delete(apiClient)
      OrganizationHelper.delete(orgId)
      AccessTokenHelper.delete(accessToken)
      UserHelper.delete(user.id)
    }
  }

  trait HasItemId {
    val itemId: VersionedId[ObjectId]
  }

  trait orgWithAccessTokenAndItem extends orgWithAccessToken with HasItemId {

    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    override def after: Any = {
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
  }

  trait sessionData extends orgWithAccessToken {
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)
    val sessionId: ObjectId = V2SessionHelper.create(itemId)

    override def before: Any = {
      super.before
    }

    override def after: Any = {
      super.after
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
      V2SessionHelper.delete(sessionId)
    }
  }

  trait user extends BeforeAfter {

    val orgId = OrganizationHelper.create("my-org")
    val user = UserHelper.create(orgId)
    val collectionId = CollectionHelper.create(orgId)

    def before: Any = {
    }

    def after: Any = {

      UserHelper.delete(user.id)
      OrganizationHelper.delete(orgId)
      CollectionHelper.delete(collectionId)
    }
  }

  trait userAndItem extends user with HasItemId {
    val itemId = ItemHelper.create(collectionId)
    override def after: Any = {
      super.after
      ItemHelper.delete(itemId)
    }
  }

  trait clientIdAndOptions extends orgWithAccessTokenAndItem {
    val clientId = apiClient.clientId
    def options: String
  }

  trait itemLoader { self: RequestBuilder with HasItemId =>

    def getCall(itemId: VersionedId[ObjectId]): Call

    lazy val result = {
      val call = getCall(itemId)
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      play.api.test.Helpers.route(makeRequest(call))(writeable).getOrElse(throw new RuntimeException("Error calling route"))
    }
  }

  trait RequestBuilder {
    def makeRequest(call: Call): Request[AnyContent]
  }

  trait TokenRequestBuilder extends RequestBuilder { self: orgWithAccessToken =>
    override def makeRequest(call: Call): Request[AnyContent] = {
      FakeRequest(call.method, s"${call.url}?access_token=${accessToken}")
    }
  }

  trait PlainRequestBuilder extends RequestBuilder {
    override def makeRequest(call: Call): Request[AnyContent] = FakeRequest(call.method, call.url)
  }

  trait SessionRequestBuilder extends RequestBuilder { self: userAndItem with SecureSocialHelpers =>

    val cookies: Seq[Cookie] = Seq(secureSocialCookie(Some(user)).get)

    override def makeRequest(call: Call): Request[AnyContent] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*)
    }
  }

  trait IdAndOptionsRequestBuilder extends RequestBuilder { self: clientIdAndOptions =>
    override def makeRequest(call: Call): Request[AnyContent] = {
      FakeRequest(call.method, s"${call.url}?clientId=$clientId&options=$options")
    }
  }

}
