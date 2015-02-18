package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models._
import org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity
import org.specs2.mutable.BeforeAfter
import play.api.Logger
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }

package object scopes {

  val logger = Logger("it.scopes")

  trait orgWithAccessToken extends BeforeAfter {
    val orgId = OrganizationHelper.create("org")
    val apiClient = ApiClientHelper.create(orgId)
    val user = UserHelper.create(orgId, "test_user")
    val accessToken = AccessTokenHelper.create(orgId, "test_user")

    println(s"[accessToken] is: $accessToken")

    def before: Any = {
      logger.debug(s"[before] apiClient ready: ${apiClient.orgId}, ${apiClient.clientId}, ${apiClient.clientSecret}")
    }

    def after: Any = {
      println("[orgWithAccessToken] after")
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

  trait HasSessionId {
    val sessionId: ObjectId
  }

  trait orgWithAccessTokenAndItem extends orgWithAccessToken with HasItemId {

    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)

    override def after: Any = {
      println("[orgWithAccessTokenAndItem] after")
      super.after
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
    }
  }

  trait orgWithAccessTokenItemAndSession extends orgWithAccessTokenAndItem with HasSessionId {
    val sessionId = V2SessionHelper.create(itemId)

    override def after: Any = {
      println("[orgWithAccessTokenAndItemAndSession] after")
      V2SessionHelper.delete(sessionId)
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

  trait userWithItemAndSession extends userAndItem with HasItemId with HasSessionId {
    def collection = "v2.itemSessions"
    val sessionId = V2SessionHelper.create(itemId, collection)
    override def after: Any = {
      super.after
      V2SessionHelper.delete(sessionId, collection)
    }
  }

  trait clientIdAndPlayerToken extends orgWithAccessTokenAndItem {
    val clientId = apiClient.clientId
    def playerToken: String
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

  trait createSession { self: RequestBuilder with HasItemId =>

    def getCall(sessionId: VersionedId[ObjectId]): Call

    lazy val req = {
      val call = getCall(itemId)
      makeRequest(call)
    }

    lazy val result = {
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      play.api.test.Helpers.route(req)(writeable).getOrElse(throw new RuntimeException("Error calling route"))
    }
  }

  trait sessionLoader { self: RequestBuilder with HasSessionId =>

    def getCall(sessionId: ObjectId): Call

    lazy val req = {
      val call = getCall(sessionId)
      val out = makeRequest(call)
      out
    }

    lazy val result = {
      implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
      val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
      play.api.test.Helpers.route(req)(writeable).getOrElse(throw new RuntimeException("Error calling route"))
    }
  }

  trait RequestBuilder {
    implicit val ct: ContentTypeOf[AnyContent] = new ContentTypeOf[AnyContent](None)
    val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
    def makeRequest(call: Call): Request[AnyContent]
  }

  trait TokenRequestBuilder extends RequestBuilder { self: orgWithAccessToken =>

    def requestBody: AnyContent = AnyContentAsEmpty

    override def makeRequest(call: Call): Request[AnyContent] = {
      println(s"[makeRequest] request body: $requestBody")
      FakeRequest(call.method, s"${call.url}?access_token=${accessToken}", FakeHeaders(), requestBody)
    }
  }

  trait PlainRequestBuilder extends RequestBuilder {
    override def makeRequest(call: Call): Request[AnyContent] = FakeRequest(call.method, call.url)
  }

  trait SessionRequestBuilder extends RequestBuilder { self: userAndItem with SecureSocialHelpers =>

    lazy val cookies: Seq[Cookie] = Seq(secureSocialCookie(Some(user)).get)

    override def makeRequest(call: Call): Request[AnyContent] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*)
    }
  }

  trait IdAndPlayerTokenRequestBuilder extends RequestBuilder { self: clientIdAndPlayerToken =>

    import PlayerTokenInQueryStringIdentity.Keys

    def requestBody: AnyContent = AnyContentAsEmpty

    def skipDecryption: Boolean

    override def makeRequest(call: Call): Request[AnyContent] = {
      val basicUrl = s"${call.url}?${Keys.apiClient}=$clientId&${Keys.playerToken}=$playerToken"
      val finalUrl = if (skipDecryption) s"$basicUrl&${Keys.skipDecryption}=true" else basicUrl
      FakeRequest(call.method, finalUrl, FakeHeaders(), requestBody)
    }
  }

}
