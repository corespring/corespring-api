package org.corespring.it

import global.Global.main
import com.amazonaws.services.s3.transfer.{ TransferManager, Upload }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.drafts.item.ItemDraftHelper
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.helpers._
import org.corespring.it.assets.{ ImageUtils, PlayerDefinitionImageUploader }
import org.corespring.models.Organization
import org.corespring.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.salat.bootstrap.CollectionNames
import org.corespring.v2.auth.identifiers.PlayerTokenIdentity.Keys
import org.specs2.specification.BeforeAfter
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.libs.Files
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }

package object scopes {

  trait WithV2SessionHelper {
    def usePreview: Boolean = false
    lazy val v2SessionHelper = V2SessionHelper(main.sessionDbConfig, usePreview)
  }

  val logger = Logger("it.scopes")

  trait orgWithAccessToken extends BeforeAfter {
    val organization = OrganizationHelper.createAndReturnOrg("org")
    val orgId = organization.id
    val apiClient = ApiClientHelper.create(orgId)
    val user = UserHelper.create(orgId, "test_user")
    val accessToken = AccessTokenHelper.create(orgId)

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

  trait orgWithAccessTokenItemAndSession
    extends orgWithAccessTokenAndItem
    with HasSessionId
    with WithV2SessionHelper {
    val sessionId = v2SessionHelper.create(itemId, orgId = Some(orgId))

    override def after: Any = {
      println("[orgWithAccessTokenAndItemAndSession] after")
      v2SessionHelper.delete(sessionId)
    }
  }

  trait sessionData extends orgWithAccessToken with WithV2SessionHelper {
    val collectionId = CollectionHelper.create(orgId)
    val itemId = ItemHelper.create(collectionId)
    val sessionId: ObjectId = v2SessionHelper.create(itemId)

    override def before: Any = {
      super.before
    }

    override def after: Any = {
      super.after
      CollectionHelper.delete(collectionId)
      ItemHelper.delete(itemId)
      v2SessionHelper.delete(sessionId)
    }
  }

  trait user extends BeforeAfter {
    val organization = OrganizationHelper.createAndReturnOrg("my-org")
    val orgId = organization.id
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
      println(s"[user][after]")
      super.after
      ItemHelper.delete(itemId)
    }
  }

  trait AddImageAndItem
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelper
    with WithV2SessionHelper {

    def imagePath: String
    lazy val logger = Logger("it.add-image-and-item")
    lazy val sessionId = v2SessionHelper.create(itemId)
    lazy val bucketName = main.bucket.bucket

    logger.info("[before]")
    logger.debug(s"[before] sessionId: $sessionId")
    logger.info(s"[before] uploading image: $imagePath, $itemId")
    PlayerDefinitionImageUploader.uploadImageAndAddToPlayerDefinition(itemId, imagePath)

    override def after: Any = {
      super.after
      logger.debug(s"[after]: delete bucket $bucketName, itemId: $itemId, session: $sessionId")
      try {
        ImageUtils.delete(itemId.id.toString)
      } catch {
        case t: Throwable => t.printStackTrace()
      }
      v2SessionHelper.delete(sessionId)
    }
  }

  trait AddSupportingMaterialImageAndItem
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelper
    with WithV2SessionHelper {

    def imagePath: String
    def materialName: String

    //TODO: Remove dependency on mongo collection - everything should be run via the service.
    lazy val itemCollection = main.db(CollectionNames.item)
    lazy val logger = Logger("it.add-supporting-material-image-and-item")
    implicit val ctx = main.context

    lazy val sessionId = v2SessionHelper.create(itemId)
    lazy val bucketName = main.bucket.bucket
    lazy val file = ImageUtils.resourcePathToFile(imagePath)

    lazy val fileBytes: Array[Byte] = {
      import java.nio.file.{ Files, Paths }
      val path = Paths.get(this.getClass.getResource(imagePath).toURI)
      Files.readAllBytes(path)
    }
    val fileName = grizzled.file.util.basename(file.getCanonicalPath)

    logger.debug(s"sessionId: $sessionId")

    require(file.exists)

    val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/materials/$materialName/$fileName"
    val sf = StoredFile(name = fileName, contentType = "image/png", storageKey = key)
    val resource = Resource(name = materialName, files = Seq(sf))
    val dbo = com.novus.salat.grater[Resource].asDBObject(resource)

    itemCollection.update(
      MongoDBObject("_id._id" -> itemId.id, "_id.version" -> itemId.version.getOrElse(0)),
      MongoDBObject("$addToSet" -> MongoDBObject("suportingMaterials" -> dbo)))

    logger.debug(s"Uploading image...: ${file.getPath} -> $key")

    ImageUtils.upload(file, key)

    override def after: Any = {
      super.after
      logger.debug(s"[after]: delete bucket $bucketName, itemId: $itemId, session: $sessionId")
      try {
        ImageUtils.delete(itemId.id.toString)
      } catch {
        case t: Throwable => t.printStackTrace()
      }
      v2SessionHelper.delete(sessionId)
    }
  }

  trait userWithItemAndSession extends userAndItem
    with HasItemId
    with HasSessionId
    with WithV2SessionHelper {

    val sessionId = v2SessionHelper.create(itemId)
    override def after: Any = {
      super.after
      v2SessionHelper.delete(sessionId)
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
      val req = makeRequest(call)
      play.api.test.Helpers.route(req)(writeable).getOrElse(throw new RuntimeException("Error calling route"))
    }
  }

  trait itemDraftLoader { self: RequestBuilder with HasItemId =>

    def organization: Organization

    def getCall(itemId: DraftId): Call

    lazy val itemDraftHelper = new ItemDraftHelper {
      override implicit def context: Context = main.context

      override def itemService: ItemService = main.itemService
    }

    lazy val draftName = scala.util.Random.alphanumeric.take(12).mkString
    lazy val draftId = DraftId(itemId.id, draftName, organization.id)

    lazy val result = {

      val createdId = itemDraftHelper.create(draftId, itemId, organization)
      require(createdId == draftId)
      val call = getCall(draftId)
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
    implicit val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
    def requestBody: AnyContent = AnyContentAsEmpty
    def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A]
  }

  trait TokenRequestBuilder extends RequestBuilder { self: orgWithAccessToken =>

    protected def mkUrl(url: String, token: String = accessToken) = {
      val separator = if (url.contains("?")) "&" else "?"
      s"$url${separator}access_token=$token"
    }

    override def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A] = {
      val separator = if (call.url.contains("?")) "&" else "?"
      FakeRequest(call.method, mkUrl(call.url), FakeHeaders(), body)
    }

    def makeJsonRequest(call: Call, json: JsValue): Request[AnyContentAsJson] = {
      FakeRequest(call.method, mkUrl(call.url)).withJsonBody(json)
    }
  }

  trait PlainRequestBuilder extends RequestBuilder {
    override def makeRequest[A <: AnyContent](call: Call, body: A = AnyContentAsEmpty): Request[A] = FakeRequest(call.method, call.url, FakeHeaders(), body)
  }

  trait SessionRequestBuilder extends RequestBuilder { self: userAndItem with SecureSocialHelper =>

    lazy val cookies: Seq[Cookie] = Seq(secureSocialCookie(Some(user)).get)

    override def makeRequest[A <: AnyContent](call: Call, body: A = AnyContentAsEmpty): Request[A] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withBody(body)
    }

    def makeFormRequest(call: Call, form: MultipartFormData[Files.TemporaryFile]): Request[AnyContentAsMultipartFormData] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withMultipartFormDataBody(form)
    }

    def makeRawRequest(call: Call, bytes: Array[Byte]) = {
      FakeRequest(call.method, call.url)
        .withCookies(cookies: _*)
        .withRawBody(bytes)
    }

    def makeJsonRequest(call: Call, json: JsValue): Request[AnyContentAsJson] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withJsonBody(json)
    }

    def makeTextRequest(call: Call, text: String): Request[AnyContentAsText] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withTextBody(text)
    }

    def makeRequestWithContentType(call: Call, body: AnyContent = AnyContentAsEmpty, contentType: String = "application/json"): Request[AnyContent] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withHeaders(("Content-Type", contentType))
    }
  }

  trait IdAndPlayerTokenRequestBuilder extends RequestBuilder { self: clientIdAndPlayerToken =>

    def skipDecryption: Boolean

    override def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A] = {
      val basicUrl = s"${call.url}?${Keys.apiClient}=$clientId&${Keys.playerToken}=$playerToken"
      val finalUrl = if (skipDecryption) s"$basicUrl&${Keys.skipDecryption}=true" else basicUrl
      FakeRequest(call.method, finalUrl, FakeHeaders(), body)
    }
  }

}
