package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import com.mongodb.util.JSON
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
import org.corespring.v2.auth.ClientIdQueryStringIdentity.Keys
import org.corespring.v2.auth._
import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2player.integration.auth.wired.{ ItemAuthWired, SessionAuthWired }
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2player.integration.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.transformers.ItemTransformer
import org.corespring.v2player.integration.urls.ComponentSetsWired
import org.corespring.v2player.integration.{ controllers => apiControllers, hooks => apiHooks }
import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }
import play.api.mvc._
import play.api.{ Configuration, Logger, Play, Mode => PlayMode }
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

  object requestIdentifiers {
    lazy val userSession = new UserSessionOrgIdentity[(ObjectId, PlayerOptions)] {
      override def secureSocialService: SecureSocialService = V2PlayerIntegration.this.secureSocialService

      override def userService: UserService = UserServiceWired

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = {
        (org.id -> PlayerOptions.ANYTHING)
      }

      override def orgService: OrgService = V2PlayerIntegration.this.orgService
    }

    lazy val token = new TokenOrgIdentity[(ObjectId, PlayerOptions)] {
      override def tokenService: TokenService = V2PlayerIntegration.this.tokenService

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = (org.id -> PlayerOptions.ANYTHING)

      override def orgService: OrgService = V2PlayerIntegration.this.orgService

    }

    lazy val clientIdAndOptsSession = new ClientIdSessionIdentity[(ObjectId, PlayerOptions)] {

      override def orgService: OrgService = V2PlayerIntegration.this.orgService

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = {
        renderOptions(rh).map(org.id -> _).getOrElse(throw new RuntimeException("No render options found"))
      }
    }

    lazy val clientIdAndOptsQueryString = new ClientIdAndOptsQueryStringWithDecrypt {

      override def orgService: OrgService = V2PlayerIntegration.this.orgService

      override def clientIdToOrgId(apiClientId: String): Option[ObjectId] = for {
        client <- ApiClient.findByKey(apiClientId)
        org <- Organization.findOneById(client.orgId)
      } yield org.id

      private def encryptionEnabled(r: RequestHeader): Boolean = {
        val m = Play.current.mode
        val acceptsFlag = m == PlayMode.Dev || m == PlayMode.Test || configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

        val enabled = if (acceptsFlag) {
          val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
          !disable
        } else true
        enabled
      }

      override def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String] = for {
        encrypter <- Some(if (encryptionEnabled(header)) AESCrypto else NullCrypto)
        orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
        out <- orgEncrypter.decrypt(encrypted)
      } yield out

    }
  }

  lazy val transformer: RequestIdentity[(ObjectId, PlayerOptions)] = new WithRequestIdentitySequence[(ObjectId, PlayerOptions)] {
    override def identifiers: Seq[OrgRequestIdentity[(ObjectId, PlayerOptions)]] = Seq(
      requestIdentifiers.clientIdAndOptsQueryString,
      requestIdentifiers.token,
      requestIdentifiers.userSession,
      requestIdentifiers.clientIdAndOptsSession)
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

  override def dataQueryHooks: DataQueryHooks = new apiHooks.DataQueryHooks {
    override def subjectQueryService: QueryService[Subject] = SubjectQueryService

    override val fieldValueJson: JsObject = {
      val dbo = FieldValue.collection.find().toSeq.head
      import play.api.libs.json.{ Json => PlayJson }
      import com.mongodb.util.{ JSON => MongoJson }
      PlayJson.parse(MongoJson.serialize(dbo)).as[JsObject]
    }

    override val standardsTreeJson: JsArray = {
      import play.api.Play.current
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = scala.io.Source.fromInputStream(is).getLines().mkString("\n")
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }
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

    override def getOrgIdAndOptions(header: RequestHeader): Validation[String, (ObjectId, PlayerOptions)] = V2PlayerIntegration.this.transformer(header)

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
