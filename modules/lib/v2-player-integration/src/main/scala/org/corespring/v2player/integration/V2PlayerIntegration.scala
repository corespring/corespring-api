package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.common.encryption.{ AESCrypto, NullCrypto }
import org.corespring.container.client.actions._
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{ Assets, DataQuery => ContainerDataQuery }
import org.corespring.container.components.model.Component
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserServiceWired
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.v2.auth.{ OrgTransformer, WithOrgTransformerSequence, WithServiceOrgTransformer }
import org.corespring.v2player.integration.auth.{ BaseAuthCheck, ItemAuth, SessionAuth }
import org.corespring.v2player.integration.cookies.Mode.Mode
import org.corespring.v2player.integration.cookies.PlayerOptions
import org.corespring.v2player.integration.hooks.{ ItemHooks => ApiItemHooks, SessionHooks => ApiSessionHooks }
import org.corespring.v2player.integration.permissions.SimpleWildcardChecker
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.{ Configuration, Logger, Mode, Play }
import securesocial.core.{ Identity, SecureSocial }

import scalaz.{ Failure, Success, Validation }

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  override def components: Seq[Component] = comps

  lazy val secureSocialService = new SecureSocialService {
    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  lazy val sessionService: MongoService = new MongoService(db("v2.itemSessions"))

  def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = new WithOrgTransformerSequence[(ObjectId, PlayerOptions)] {
    //TODO: Add org transformers here..
    override def transformers: Seq[WithServiceOrgTransformer[(ObjectId, PlayerOptions)]] = Seq.empty
  }

  def encryptionEnabled(r: RequestHeader): Boolean = {
    val acceptsFlag = Play.current.mode == Mode.Dev || configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

    val enabled = if (acceptsFlag) {
      val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
      !disable
    } else true
    enabled
  }

  def decrypt(request: RequestHeader, orgId: ObjectId, contents: String): Option[String] = for {
    encrypter <- Some(if (encryptionEnabled(request)) AESCrypto else NullCrypto)
    orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
    out <- orgEncrypter.decrypt(contents)
  } yield out

  def toOrgId(apiClientId: String): Option[ObjectId] = {
    logger.debug(s"[toOrgId] find org for apiClient: $apiClientId")
    val client = ApiClient.findByKey(apiClientId)

    if (client.isEmpty) {
      logger.warn(s"[toOrgId] can't find org for $apiClientId")
    }
    client.map(_.orgId)
  }

  override def componentUrls: ComponentUrls = ???

  override def assets: Assets = ???

  override def dataQuery: ContainerDataQuery = ???

  lazy val baseAuthCheck = new BaseAuthCheck(
    V2PlayerIntegration.this.secureSocialService,
    UserServiceWired,
    V2PlayerIntegration.this.sessionService,
    ItemServiceWired,
    Organization) {

    val permissionGranter = new SimpleWildcardChecker()

    override def hasPermissions(
      itemId: String,
      sessionId: Option[String],
      mode: Mode,
      options: PlayerOptions): Validation[String, Boolean] = permissionGranter.allow(itemId, sessionId, mode, options).fold(Failure(_), Success(_))

    override def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String] = V2PlayerIntegration.this.decrypt(request, orgId, encrypted)

    override def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = V2PlayerIntegration.this.transformer

    override def toOrgId(apiClientId: String): Option[ObjectId] = V2PlayerIntegration.this.toOrgId(apiClientId)
  }

  override def sessionHooks: SessionHooks = new ApiSessionHooks {
    override def auth: SessionAuth = ???

    override def itemService: ItemService = ???

    override def transformItem: (Item) => JsValue = ???

    override def sessionService: MongoService = ???
  }

  override def itemHooks: ItemHooks = new ApiItemHooks {
    override def itemService: ItemService = ???

    override def transform: (Item) => JsValue = ???

    override def orgService: OrganizationService = ???

    override def auth: ItemAuth = ???
  }

  override def playerLauncherActions: PlayerLauncherActions[AnyContent] = ???

  override def catalogActions: CatalogActions[AnyContent] = ???

  override def playerActions: PlayerActions[AnyContent] = ???

  override def editorActions: EditorActions[AnyContent] = ???
}
