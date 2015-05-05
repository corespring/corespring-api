package org.corespring.v2.player

import java.io.File

import com.amazonaws.auth.{ BasicAWSCredentials, AWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ Upload, TransferManager }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.drafts.item.ItemDraftHelper
import org.corespring.drafts.item.models.DraftId
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.core.models.{ mongoContext, Organization }
import org.corespring.platform.core.services.item.ItemServiceWired
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
    val organization = OrganizationHelper.createAndReturnOrg("org")
    val orgId = organization.id
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

  trait S3Helper {
    def client: AmazonS3Client
    def bucketName: String
    def logger: Logger

    def rmAssets(key: String) = {
      import scala.collection.JavaConversions._
      client.listObjects(bucketName, key).getObjectSummaries.foreach { k =>
        logger.debug(s"rm key=${k.getKey}")
        client.deleteObject(bucketName, k.getKey)
      }
      client.deleteObject(bucketName, key)
    }
  }

  class AddImageAndItem(imagePath: String)
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelpers
    with S3Helper {

    lazy val logger = Logger("v2player.test")
    lazy val credentials: AWSCredentials = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val sessionId = V2SessionHelper.create(itemId, V2SessionHelper.v2ItemSessions)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {
      import org.corespring.platform.core.models.mongoContext._

      super.before

      logger.debug(s"sessionId: $sessionId")

      val file = new File(imagePath)
      require(file.exists)

      val item = ItemServiceWired.findOneById(itemId).get
      val name = grizzled.file.util.basename(file.getCanonicalPath)
      val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/data/${name}"

      val sf = StoredFile(name = name, contentType = "image/png", storageKey = key)
      val dbo = com.novus.salat.grater[StoredFile].asDBObject(sf)

      ItemServiceWired.collection.update(
        MongoDBObject("_id._id" -> itemId.id, "_id.version" -> itemId.version.getOrElse(0)),
        MongoDBObject("$addToSet" -> MongoDBObject("data.files" -> dbo)))

      val reItem = ItemServiceWired.findOneById(itemId)
      logger.debug(s"Saved item in mongo as: ${reItem}")

      logger.debug(s"Uploading image...: ${file.getPath} -> $key")
      val upload: Upload = tm.upload(bucketName, key, file)
      upload.waitForUploadResult()
    }

    override def after: Any = {
      super.after
      logger.debug(s"[after]: delete bucket $bucketName, itemId: $itemId, session: $sessionId")
      try {
        rmAssets(itemId.id.toString)
      } catch {
        case t: Throwable => t.printStackTrace()
      }
      V2SessionHelper.delete(sessionId)
    }
  }

  class AddSupportingMaterialImageAndItem(imagePath: String, val materialName: String)
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelpers
    with S3Helper {

    lazy val logger = Logger("v2player.test")
    lazy val credentials: AWSCredentials = new BasicAWSCredentials(AppConfig.amazonKey, AppConfig.amazonSecret)
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val sessionId = V2SessionHelper.create(itemId, V2SessionHelper.v2ItemSessions)
    lazy val bucketName = AppConfig.assetsBucket
    lazy val file = new File(imagePath)

    lazy val fileBytes: Array[Byte] = {
      import java.nio.file.Files
      import java.nio.file.Paths
      val path = Paths.get(imagePath)
      Files.readAllBytes(path)
    }
    val fileName = grizzled.file.util.basename(file.getCanonicalPath)

    override def before: Any = {
      import org.corespring.platform.core.models.mongoContext._

      super.before

      logger.debug(s"sessionId: $sessionId")

      require(file.exists)

      val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/materials/$materialName/$fileName"
      val sf = StoredFile(name = fileName, contentType = "image/png", storageKey = key)
      val resource = Resource(name = materialName, files = Seq(sf))
      val dbo = com.novus.salat.grater[Resource].asDBObject(resource)

      ItemServiceWired.collection.update(
        MongoDBObject("_id._id" -> itemId.id, "_id.version" -> itemId.version.getOrElse(0)),
        MongoDBObject("$addToSet" -> MongoDBObject("suportingMaterials" -> dbo)))

      logger.debug(s"Uploading image...: ${file.getPath} -> $key")
      val upload: Upload = tm.upload(bucketName, key, file)
      upload.waitForUploadResult()
    }

    override def after: Any = {
      super.after
      logger.debug(s"[after]: delete bucket $bucketName, itemId: $itemId, session: $sessionId")
      try {
        rmAssets(itemId.id.toString)
      } catch {
        case t: Throwable => t.printStackTrace()
      }
      V2SessionHelper.delete(sessionId)
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

  trait itemDraftLoader { self: RequestBuilder with HasItemId =>

    def organization: Organization

    def getCall(itemId: DraftId): Call

    lazy val itemDraftHelper = new ItemDraftHelper {
      override implicit def context: Context = mongoContext.context
    }

    def draftName = scala.util.Random.alphanumeric.take(12).mkString

    lazy val result = {

      val draftId = itemDraftHelper.create(DraftId(itemId.id, draftName, organization.id), itemId, organization)
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
    val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
    def makeRequest(call: Call): Request[AnyContent]
  }

  trait TokenRequestBuilder extends RequestBuilder { self: orgWithAccessToken =>

    def requestBody: AnyContent = AnyContentAsEmpty

    override def makeRequest(call: Call): Request[AnyContent] = {
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
