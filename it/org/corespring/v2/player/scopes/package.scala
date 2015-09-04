package org.corespring.v2.player

import java.io.File

import com.amazonaws.auth.{ BasicAWSCredentials, AWSCredentials }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ Upload, TransferManager }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId
import org.corespring.common.aws.AwsUtil
import org.corespring.common.config.{ SessionDbConfig, AppConfig }
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
import play.api.libs.Files
import play.api.libs.json.JsValue
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
    val sessionId = V2SessionHelper.create(itemId, orgId = Some(orgId))

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

  object ImageUtils {

    lazy val credentials: AWSCredentials = AwsUtil.credentials()
    lazy val client = new AmazonS3Client(credentials)
    lazy val bucket = AppConfig.assetsBucket

    def exists(path: String): Boolean = {
      try {
        val o = client.getObject(bucket, path)
        println(o.getKey)
        true
      } catch {
        case _: Throwable => false
      }
    }

    def list(path: String): Seq[String] = {
      import scala.collection.JavaConversions._
      val l = client.listObjects(bucket, path)
      l.getObjectSummaries.map { s => s.getKey }
    }

    def delete(path: String) = {
      list(path).foreach { key =>
        println(s"[delete] $key")
        client.deleteObject(bucket, key)
      }
      println(s"[delete] $path")
      client.deleteObject(bucket, path)
    }
  }

  object ImageUploader {

    import mongoContext.context

    lazy val logger = Logger("v2player.test.ImageUploader")

    lazy val credentials: AWSCredentials = AwsUtil.credentials()
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)
    lazy val bucketName = AppConfig.assetsBucket

    def uploadImage(itemId: VersionedId[ObjectId], imagePath: String) = {
      val file = new File(imagePath)
      require(file.exists)

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
      itemId
    }

    def imageData(imagePath: String): Array[Byte] = {
      val file = new File(imagePath)
      require(file.exists)
      require(Seq("jpg", "png").exists(s => file.getName.endsWith(s)))
      val bytes = IOUtils.toByteArray(file.toURI)
      bytes
    }

  }

  class AddImageAndItem(imagePath: String)
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelpers
    with S3Helper {

    lazy val logger = Logger("v2player.test")
    lazy val credentials: AWSCredentials = AwsUtil.credentials()
    lazy val tm: TransferManager = new TransferManager(credentials)
    lazy val client = new AmazonS3Client(credentials)

    lazy val sessionId = V2SessionHelper.create(itemId, V2SessionHelper.v2ItemSessions)
    lazy val bucketName = AppConfig.assetsBucket

    override def before: Any = {
      import org.corespring.platform.core.models.mongoContext._

      super.before
      logger.debug(s"sessionId: $sessionId")
      ImageUploader.uploadImage(itemId, imagePath)
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

  trait AddSupportingMaterialImageAndItem
    extends userAndItem
    with SessionRequestBuilder
    with SecureSocialHelpers
    with S3Helper {

    def imagePath: String
    def materialName: String

    lazy val logger = Logger("v2player.test")

    lazy val credentials: AWSCredentials = AwsUtil.credentials()
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

    lazy val fileName = grizzled.file.util.basename(file.getCanonicalPath)

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
    def collection = SessionDbConfig.sessionTable
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
    val writeable: Writeable[AnyContent] = Writeable[AnyContent]((c: AnyContent) => Array[Byte]())
    def requestBody: AnyContent = AnyContentAsEmpty
    def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A]
  }

  trait TokenRequestBuilder extends RequestBuilder { self: orgWithAccessToken =>

    override def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A] = {
      FakeRequest(call.method, s"${call.url}?access_token=${accessToken}", FakeHeaders(), body)
    }
  }

  trait PlainRequestBuilder extends RequestBuilder {
    override def makeRequest[A <: AnyContent](call: Call, body: A = AnyContentAsEmpty): Request[A] = FakeRequest(call.method, call.url, FakeHeaders(), body)
  }

  trait SessionRequestBuilder extends RequestBuilder { self: userAndItem with SecureSocialHelpers =>

    lazy val cookies: Seq[Cookie] = Seq(secureSocialCookie(Some(user)).get)

    override def makeRequest[A <: AnyContent](call: Call, body: A = AnyContentAsEmpty): Request[A] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withBody(body)
    }

    def makeFormRequest(call: Call, form: MultipartFormData[Files.TemporaryFile]): Request[AnyContentAsMultipartFormData] = {
      FakeRequest(call.method, call.url).withCookies(cookies: _*).withMultipartFormDataBody(form)
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

    import PlayerTokenInQueryStringIdentity.Keys

    def skipDecryption: Boolean

    override def makeRequest[A <: AnyContent](call: Call, body: A = requestBody): Request[A] = {
      val basicUrl = s"${call.url}?${Keys.apiClient}=$clientId&${Keys.playerToken}=$playerToken"
      val finalUrl = if (skipDecryption) s"$basicUrl&${Keys.skipDecryption}=true" else basicUrl
      FakeRequest(call.method, finalUrl, FakeHeaders(), body)
    }
  }

}
