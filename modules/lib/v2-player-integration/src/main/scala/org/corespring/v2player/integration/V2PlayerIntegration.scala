package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import org.corespring.common.config.AppConfig
import org.corespring.common.encryption.{ AESCrypto, NullCrypto }
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{ Assets, DataQuery => ContainerDataQuery }
import org.corespring.container.client.hooks._
import org.corespring.container.components.model.Component
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.auth.{ AccessToken, ApiClient }
import org.corespring.platform.core.models.item.{ FieldValue, Item, ItemTransformationCache, PlayItemTransformationCache }
import org.corespring.platform.core.models.{ Organization, Subject }
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.services.{ QueryService, SubjectQueryService, UserService, UserServiceWired }
import org.corespring.v2.auth._
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2player.integration.auth.wired.{ ItemAuthWired, SessionAuthWired }
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2player.integration.cookies.Mode.Mode
import org.corespring.v2player.integration.cookies.PlayerOptions
import org.corespring.v2player.integration.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.transformers.ItemTransformer
import org.corespring.v2player.integration.urls.ComponentSetsWired
import org.corespring.v2player.integration.{ controllers => apiControllers, hooks => apiHooks }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.{ Configuration, Logger, Mode, Play }
import securesocial.core.{ Identity, SecureSocial }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  def ec: ExecutionContext = ExecutionContext.Implicits.global

  override def components: Seq[Component] = comps

  lazy val secureSocialService = new SecureSocialService {
    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  protected val tokenService = new TokenService {
    override def orgForToken(token: String): Option[Organization] = {
      AccessToken.findByToken(token).map(t => orgService.org(t.organization)).flatten
    }
  }

  lazy val itemTransformer = new ItemTransformer {
    override def cache: ItemTransformationCache = new PlayItemTransformationCache()
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

  lazy val sessionService: MongoService = new MongoService(db("v2.itemSessions"))

  object requestTransformers {
    lazy val sessionBased = new SessionBasedRequestTransformer[(ObjectId, PlayerOptions)] {
      override def secureSocialService: SecureSocialService = V2PlayerIntegration.this.secureSocialService

      override def userService: UserService = UserServiceWired

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = {
        (org.id -> PlayerOptions.ANYTHING)
      }

      override def orgService: OrgService = V2PlayerIntegration.this.orgService
    }

    lazy val token = new TokenBasedRequestTransformer[(ObjectId, PlayerOptions)] {
      override def tokenService: TokenService = V2PlayerIntegration.this.tokenService

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = (org.id -> PlayerOptions.ANYTHING)

      override def orgService: OrgService = V2PlayerIntegration.this.orgService
    }

    lazy val clientIdAndOpts = new ClientIdAndOptionsTransformer[(ObjectId, PlayerOptions)] {

      override def clientIdToOrgId(apiClientId: String): Option[ObjectId] = for {
        client <- ApiClient.findByKey(apiClientId)
        org <- Organization.findOneById(client.orgId)
      } yield org.id

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = (org.id -> toPlayerOptions(org.id, rh))

      def encryptionEnabled(r: RequestHeader): Boolean = {
        val m = Play.current.mode
        val acceptsFlag = m == Mode.Dev || m == Mode.Test || configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

        val enabled = if (acceptsFlag) {
          val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
          !disable
        } else true
        enabled
      }

      def decrypt(orgId: ObjectId, header: RequestHeader): Option[String] = for {
        encrypted <- header.queryString.get("options").map(_.head)
        encrypter <- Some(if (encryptionEnabled(header)) AESCrypto else NullCrypto)
        orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
        out <- orgEncrypter.decrypt(encrypted)
      } yield out

      def toOrgId(apiClientId: String): Option[ObjectId] = {
        logger.debug(s"[toOrgId] find org for apiClient: $apiClientId")
        val client = ApiClient.findByKey(apiClientId)

        if (client.isEmpty) {
          logger.warn(s"[toOrgId] can't find org for $apiClientId")
        }
        client.map(_.orgId)
      }

      private def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions = {
        for {
          optsString <- rh.queryString.get("options").map(_.head)
          decrypted <- decrypt(orgId, rh)
          json <- try {
            Some(Json.parse(decrypted))
          } catch {
            case _: Throwable => None
          }
          playerOptions <- json.asOpt[PlayerOptions]
        } yield playerOptions
      }.getOrElse(PlayerOptions.NOTHING)

      override def orgService: OrgService = V2PlayerIntegration.this.orgService
    }
  }

  lazy val transformer: OrgTransformer[(ObjectId, PlayerOptions)] = new WithOrgTransformerSequence[(ObjectId, PlayerOptions)] {
    //TODO: Add org transformers here..
    override def transformers: Seq[WithServiceOrgTransformer[(ObjectId, PlayerOptions)]] = Seq(
      requestTransformers.sessionBased,
      requestTransformers.token,
      requestTransformers.clientIdAndOpts)
  }

  lazy val itemAuth = new ItemAuthWired {
    override def orgService: OrganizationService = Organization

    override def itemService: ItemService = ItemServiceWired

    override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, sessionId, mode, options).fold(Failure(_), Success(_))
    }

    override def getOrgIdAndOptions(request: RequestHeader): Validation[String, (ObjectId, PlayerOptions)] = {
      V2PlayerIntegration.this.transformer(request)
    }
  }

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret

  lazy val playS3 = new ConcreteS3Service(key, secret)

  override def componentUrls: ComponentUrls = new ComponentSetsWired {
    override def allComponents: Seq[Component] = V2PlayerIntegration.this.components
  }

  override def assets: Assets = new apiControllers.Assets {
    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def hooks: AssetHooks = new apiHooks.AssetHooks {
      override def itemService: ItemService = ItemServiceWired

      override def bucket: String = AppConfig.assetsBucket

      override def s3: S3Service = playS3

      override def auth: ItemAuth = V2PlayerIntegration.this.itemAuth

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global
    }

    override def itemAuth: ItemAuth = V2PlayerIntegration.this.itemAuth
  }

  override def dataQuery: ContainerDataQuery = new apiControllers.DataQuery {
    override def subjectQueryService: QueryService[Subject] = SubjectQueryService

    override def fieldValues: FieldValue = FieldValue.findAll().toSeq.head
  }

  lazy val sessionAuth: SessionAuth = new SessionAuthWired {
    override def itemAuth: ItemAuth = V2PlayerIntegration.this.itemAuth

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService
  }

  override def sessionHooks: SessionHooks = new apiHooks.SessionHooks {
    override def auth: SessionAuth = V2PlayerIntegration.this.sessionAuth

    override def itemService: ItemService = ItemServiceWired

    override def transformItem: (Item) => JsValue = itemTransformer.transformToV2Json

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec
  }

  override def itemHooks: ItemHooks = new apiHooks.ItemHooks {

    override def transform: (Item) => JsValue = itemTransformer.transformToV2Json

    override def auth: ItemAuth = V2PlayerIntegration.this.itemAuth

    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec
  }

  override def playerLauncherHooks: PlayerLauncherHooks = new apiHooks.PlayerLauncherHooks {
    override def secureSocialService: SecureSocialService = V2PlayerIntegration.this.secureSocialService

    override def getOrgIdAndOptions(header: RequestHeader): Validation[String, (ObjectId, PlayerOptions)] = ???

    override def userService: UserService = UserServiceWired

    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

  }

  override def catalogHooks: CatalogHooks = new apiHooks.CatalogHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = itemTransformer.transformToV2Json
  }

  override def playerHooks: PlayerHooks = new apiHooks.PlayerHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override def itemService: ItemService = ItemServiceWired

    override def transformItem: (Item) => JsValue = itemTransformer.transformToV2Json

    override def auth: SessionAuth = V2PlayerIntegration.this.sessionAuth
  }

  override def editorHooks: EditorHooks = new apiHooks.EditorHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = itemTransformer.transformToV2Json

    override def auth: ItemAuth = V2PlayerIntegration.this.itemAuth
  }
}
