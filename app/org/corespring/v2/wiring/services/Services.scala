package org.corespring.v2.wiring.services

import com.mongodb.casbah.{MongoCollection, MongoDB}
import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.drafts.item.models.ItemDraft
import org.corespring.drafts.item.services.{CommitService, ItemDraftService}
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.caching.SimpleCache
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.{ OrgEncrypter, OrgEncryptionService }
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.{Permission, ApiClient, ApiClientService, AccessToken}
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.V2ApiServices
import org.corespring.v2.auth.encryption.CachingOrgEncryptionService
import org.corespring.v2.auth.services.caching.CachingTokenService
import org.corespring.v2.auth.{Auth, ItemAuth, SessionAuth}
import org.corespring.v2.auth.models.{ Mode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.auth.wired.{ SessionAuthWired, ItemAuthWired }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.v2.player.permissions.SimpleWildcardChecker
import play.api.Configuration
import play.api.mvc.RequestHeader
import securesocial.core.{ Identity, SecureSocial }

import scalaz.{ Success, Failure, Validation }

class Services(cacheConfig: Configuration, db: MongoDB, itemTransformer: ItemTransformer) extends V2ApiServices{

  private lazy val logger = V2LoggerFactory.getLogger(this.getClass.getSimpleName)

  lazy val mainSessionService: MongoService = new MongoService(db("v2.itemSessions"))

  override val sessionService: MongoService = mainSessionService

  override val itemService: ItemService = ItemServiceWired

  override val draftService: ItemDraftService = new ItemDraftService {
    override def collection: MongoCollection = db("drafts.items")
  }

  lazy val previewSessionService: MongoService = new MongoService(db("v2.itemSessions_preview"))


  lazy val itemCommitService : CommitService = new CommitService{
    override def collection: MongoCollection = db("drafts.item_commits")
  }

  lazy val secureSocialService = new SecureSocialService {
    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  /** A wrapper around organization */
  lazy val orgService = new OrgService {
    override def defaultCollection(o: Organization): Option[ObjectId] = {
      Organization.getDefaultCollection(o.id) match {
        case Right(coll) => Some(coll.id)
        case Left(e) => None
      }
    }

    override def org(id: ObjectId): Option[Organization] = Organization.findOneById(id)
  }

  lazy val orgEncryptionService: OrgEncryptionService = {
    val basicEncrypter = new OrgEncrypter(AESCrypto)

    if (cacheConfig.getBoolean("OrgEncryptionService.enabled").getOrElse(false)) {
      logger.debug(s"orgEncryptionService - using cached OrgEncryptionService")
      import scala.concurrent.duration._
      val ttl = cacheConfig.getInt("OrgEncryptionService.ttl-in-minutes").getOrElse(10)
      new CachingOrgEncryptionService(basicEncrypter, ttl.minutes)
    } else {
      logger.debug(s"orgEncryptionService - using non caching OrgEncryptionService")
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

  lazy val itemDraftAuth = new Auth[ItemDraft,OrgAndOpts,ObjectId]{

    def orgService: OrganizationService = Organization

    def hasPermissions(itemId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, None, Mode.evaluate, settings).fold(m => Failure(permissionNotGranted(m)), Success(_))
    }

    private def canWithPermission(uid:String, p:Permission)(implicit identity:OrgAndOpts) : Validation[V2Error,ItemDraft] = {
      import scalaz.Scalaz._
      for{
        d <- draftService.load(new ObjectId(uid)).toSuccess(generalError("Can't find draft"))
        collectionId <- d.src.data.collectionId.toSuccess(noCollectionIdForItem(d.src.data.id))
        canAccess <- if(orgService.canAccessCollection(identity.org, new ObjectId(collectionId), p)){
          Success(true)
        } else {
          Failure(orgCantAccessCollection(identity.org.id, collectionId, Permission.Read.name))
        }
        hasPermissions <- hasPermissions(d.src.data.id.toString, identity.opts)
      } yield d
    }

    override def loadForRead(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, ItemDraft] = {
      canWithPermission(id, Permission.Read)
    }

    override def loadForWrite(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, ItemDraft] = {
      canWithPermission(id, Permission.Write)
    }

    private def saveOrInsert(data:ItemDraft, oid:ObjectId)(implicit identity:OrgAndOpts) = {
      for{
        collectionId <- data.src.data.collectionId
        _ <- if(orgService.canAccessCollection(identity.org, new ObjectId(collectionId), Permission.Write)) Some(true) else None
        _ <- hasPermissions(data.src.data.id.toString, identity.opts).toOption
        result <- Some(draftService.save(data.copy(id = oid)))
        _ <- if(result.getLastError.ok) Some(true) else None
      } yield oid
    }

    override def insert(data: ItemDraft)(implicit identity: OrgAndOpts): Option[ObjectId] = saveOrInsert(data, ObjectId.get)

    override def save(data: ItemDraft, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = saveOrInsert(data, data.id)
  }

  lazy val itemAuth = new ItemAuthWired {
    override def orgService: OrganizationService = Organization

    override def itemService: ItemService = ItemServiceWired

    override def hasPermissions(itemId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, None, Mode.evaluate, settings).fold(m => Failure(permissionNotGranted(m)), Success(_))
    }

    override def itemTransformer: ItemTransformer = Services.this.itemTransformer
  }

  lazy val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = new SessionAuthWired {
    override def itemAuth: ItemAuth[OrgAndOpts] = Services.this.itemAuth

    override def mainSessionService: MongoService = Services.this.mainSessionService

    override def hasPermissions(itemId: String, sessionId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, Some(sessionId), Mode.evaluate, settings).fold(m => Failure(permissionNotGranted(m)), Success(_))
    }

    /**
     * The preview session service holds 'preview' sessions -
     * This service is used when the identity -> AuthMode == UserSession
     * @return
     */
    override def previewSessionService: MongoService = Services.this.previewSessionService

    override def itemTransformer: ItemTransformer = Services.this.itemTransformer
  }
}
