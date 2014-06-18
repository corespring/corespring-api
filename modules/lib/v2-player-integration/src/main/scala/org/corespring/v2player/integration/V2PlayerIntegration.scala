package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import org.corespring.common.config.AppConfig
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{ Assets, DataQuery => ContainerDataQuery }
import org.corespring.container.client.hooks._
import org.corespring.container.components.model.Component
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.item.{ FieldValue, Item }
import org.corespring.platform.core.models.{ Organization, Subject }
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.services.{ QueryService, SubjectQueryService, UserService, UserServiceWired }
import org.corespring.v2.auth.{ OrgTransformer, WithOrgTransformerSequence, WithServiceOrgTransformer }
import org.corespring.v2player.integration.auth.wired.{ ItemAuthWired, SessionAuthWired }
import org.corespring.v2player.integration.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2player.integration.cookies.Mode.Mode
import org.corespring.v2player.integration.cookies.PlayerOptions
import org.corespring.v2player.integration.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.transformers.ItemTransformer
import org.corespring.v2player.integration.urls.ComponentSetsWired
import org.corespring.v2player.integration.{ controllers => apiControllers, hooks => apiHooks }
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.{ Configuration, Logger }
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

  lazy val sessionService: MongoService = new MongoService(db("v2.itemSessions"))

  lazy val transformer: OrgTransformer[(ObjectId, PlayerOptions)] = new WithOrgTransformerSequence[(ObjectId, PlayerOptions)] {
    //TODO: Add org transformers here..
    override def transformers: Seq[WithServiceOrgTransformer[(ObjectId, PlayerOptions)]] = Seq.empty
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

  //  def encryptionEnabled(r: RequestHeader): Boolean = {
  //    val acceptsFlag = Play.current.mode == Mode.Dev || configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)
  //
  //    val enabled = if (acceptsFlag) {
  //      val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
  //      !disable
  //    } else true
  //    enabled
  //  }
  //
  //  def decrypt(request: RequestHeader, orgId: ObjectId, contents: String): Option[String] = for {
  //    encrypter <- Some(if (encryptionEnabled(request)) AESCrypto else NullCrypto)
  //    orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
  //    out <- orgEncrypter.decrypt(contents)
  //  } yield out
  //
  //  def toOrgId(apiClientId: String): Option[ObjectId] = {
  //    logger.debug(s"[toOrgId] find org for apiClient: $apiClientId")
  //    val client = ApiClient.findByKey(apiClientId)
  //
  //    if (client.isEmpty) {
  //      logger.warn(s"[toOrgId] can't find org for $apiClientId")
  //    }
  //    client.map(_.orgId)
  //  }

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

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

    override def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec
  }

  override def itemHooks: ItemHooks = new apiHooks.ItemHooks {
    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def orgService: OrganizationService = Organization

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

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json
  }

  override def playerHooks: PlayerHooks = new apiHooks.PlayerHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override def itemService: ItemService = ItemServiceWired

    override def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: SessionAuth = V2PlayerIntegration.this.sessionAuth
  }

  override def editorHooks: EditorHooks = new apiHooks.EditorHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: ItemAuth = V2PlayerIntegration.this.itemAuth
  }
}
