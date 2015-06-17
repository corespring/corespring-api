package org.corespring.v2.wiring.services

import com.amazonaws.services.s3.AmazonS3Client
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{ MongoCollection, MongoDB }
import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.drafts.item.models.OrgAndUser
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.item.{ ItemDraftAssets, ItemDrafts, S3ItemDraftAssets }
import org.corespring.platform.core.caching.SimpleCache
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.{ ApiClientEncrypter, ApiClientEncryptionService }
import org.corespring.platform.core.models.{ ContentCollection, Organization }
import org.corespring.platform.core.models.auth.{ AccessToken, ApiClient, ApiClientService, Permission }
import org.corespring.platform.core.models.item.{ ItemType, PlayerDefinition }
import org.corespring.platform.core.services.item._
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.V2ApiServices
import org.corespring.v2.auth._
import org.corespring.v2.auth.encryption.CachingApiClientEncryptionService
import org.corespring.v2.auth.models.{ Mode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.services.caching.CachingTokenService
import org.corespring.v2.auth.services.{SessionDbService, ContentCollectionService, OrgService, TokenService}
import org.corespring.v2.auth.wired._
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.wiring.SessionDbServiceFactory
import play.api.Configuration
import play.api.mvc.RequestHeader
import securesocial.core.{ Identity, SecureSocial }

import scalaz.{ Failure, Success, Validation }

class Services(cacheConfig: Configuration, db: MongoDB, itemTransformer: ItemTransformer, s3: AmazonS3Client,
  bucket: String, sessionDbServiceFactory: SessionDbServiceFactory) extends V2ApiServices {

  private lazy val logger = V2LoggerFactory.getLogger(this.getClass.getSimpleName)

  lazy val mainSessionService: SessionDbService = sessionDbServiceFactory.create("v2.itemSessions")

  override val sessionService: SessionDbService = mainSessionService

  override val itemService: ItemService with ItemPublishingService = ItemServiceWired

  override val itemType: ItemType = ItemType

  override val itemIndexService: ItemIndexService = ElasticSearchItemIndexService

  override def draftsBackend: ItemDrafts = new ItemDrafts {
    override def itemService = Services.this.itemService

    override val draftService: ItemDraftService = new ItemDraftService {
      override def collection: MongoCollection = db("drafts.items")
    }

    override def assets: ItemDraftAssets = new S3ItemDraftAssets {
      override def bucket: String = Services.this.bucket

      override def s3: AmazonS3Client = Services.this.s3

    }

    override def commitService: CommitService = Services.this.itemCommitService

    override protected def userCanCreateDraft(itemId: ObjectId, user: OrgAndUser): Boolean = {
      hasWriteAccess(itemId, user)
    }

    override protected def userCanDeleteDrafts(itemId: ObjectId, user: OrgAndUser): Boolean = {
      hasWriteAccess(itemId, user)
    }

    private def hasWriteAccess(itemId: ObjectId, user: OrgAndUser) = {
      itemService.collection.findOne(MongoDBObject("_id._id" -> itemId), MongoDBObject("collectionId" -> 1)).map { dbo =>
        try {
          val collectionId = dbo.get("collectionId").asInstanceOf[String]
          val canAccess = Organization.canAccessCollection(user.org.id, new ObjectId(collectionId), Permission.Write)
          logger.debug(s"function=hasWriteAcces, canAccess=$canAccess")
          canAccess
        } catch {
          case t: Throwable => false
        }
      }.getOrElse(false)
    }
  }

  lazy val previewSessionService: SessionDbService = sessionDbServiceFactory.create("v2.itemSessions_preview")

  lazy val itemCommitService: CommitService = new CommitService {
    override def collection: MongoCollection = db("drafts.item_commits")
  }

  lazy val secureSocialService = new SecureSocialService {
    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  /** A wrapper around ContentCollection */
  lazy val colService = new ContentCollectionService {

    override def getCollections(o: Organization, p: Permission) = {
      ContentCollection.getCollections(o.id, p) match {
        case Right(coll) => coll
        case Left(e) => Seq.empty
      }
    }
  }

  /** A wrapper around organization */
  lazy val orgService = new OrgService {

    override def defaultCollection(oid: ObjectId): Option[ObjectId] = {
      Organization.getDefaultCollection(oid) match {
        case Right(coll) => Some(coll.id)
        case Left(e) => None
      }
    }

    override def defaultCollection(o: Organization): Option[ObjectId] = {
      defaultCollection(o.id)
    }

    override def org(id: ObjectId): Option[Organization] = Organization.findOneById(id)
  }

  lazy val apiClientEncryptionService: ApiClientEncryptionService = {
    val basicEncrypter = new ApiClientEncrypter(AESCrypto)

    if (cacheConfig.getBoolean("ApiClientEncryptionService.enabled").getOrElse(false)) {
      logger.debug(s"apiClientEncryptionService - using cached ApiClientEncryptionService")
      import scala.concurrent.duration._
      val ttl = cacheConfig.getInt("ApiClientEncryptionService.ttl-in-minutes").getOrElse(10)
      new CachingApiClientEncryptionService(basicEncrypter, ttl.minutes)
    } else {
      logger.debug(s"apiClientEncryptionService - using non caching ApiClientEncryptionService")
      basicEncrypter
    }
  }

  lazy val apiClientService = if (cacheConfig.getBoolean("ApiClientService.enabled").getOrElse(false)) {
    new ApiClientService {
      val localCache = new SimpleCache[ApiClient] {
        override def timeToLiveInMinutes = cacheConfig.getDouble("ApiClientService.ttl-in-minutes").getOrElse(3)
      }
      override def findByKey(key: String): Option[ApiClient] = localCache.get(key).orElse {
        val out = ApiClient.findByKey(key)
        out.foreach(localCache.set(key, _))
        out
      }
    }
  } else {
    ApiClient
  }

  private lazy val mainTokenService = new TokenService {
    import scalaz.Scalaz._
    override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] = for {
      accessToken <- AccessToken.findByToken(token).toSuccess(invalidToken(rh))
      _ = logger.debug(s"val=mainTokenService accessToken=$token")
      unexpiredToken <- if (accessToken.isExpired) Failure(expiredToken(rh)) else Success(accessToken)
      _ = logger.debug(s"val=mainTokenService accessToken=$token - is an unexpired token")
      org <- orgService.org(unexpiredToken.organization).toSuccess(noOrgForToken(rh))
      _ = logger.debug(s"val=mainTokenService accessToken=$token org=$org")
    } yield org
  }

  lazy val tokenService = if (cacheConfig.getBoolean("TokenService.enabled").getOrElse(false)) {
    new CachingTokenService {
      override def underlying: TokenService = mainTokenService
      override def timeToLiveInMinutes = cacheConfig.getLong("TokenService.ttl-in-minutes").getOrElse(1)
    }
  } else {
    mainTokenService
  }

  lazy val itemAccess = new ItemAccess {
    override def orgService: OrganizationService = Organization
  }

  lazy val itemAuth = new ItemAuthWired {

    override def itemService: ItemService = ItemServiceWired

    override def itemTransformer: ItemTransformer = Services.this.itemTransformer

    override def access: ItemAccess = Services.this.itemAccess
  }

  lazy val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = new SessionAuthWired {
    override def itemAuth: ItemAuth[OrgAndOpts] = Services.this.itemAuth

    override def mainSessionService: SessionDbService = Services.this.mainSessionService

    override def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] =
      AccessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)

    override def previewSessionService: SessionDbService = Services.this.previewSessionService

    override def itemTransformer: ItemTransformer = Services.this.itemTransformer
  }
}
