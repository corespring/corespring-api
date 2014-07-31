package org.corespring.v2.player

import java.io.File

import org.corespring.v2.log.V2LoggerFactory

import scala.concurrent.ExecutionContext

import com.mongodb.casbah.MongoDB
import com.typesafe.config.ConfigFactory
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import org.corespring.common.config.AppConfig
import org.corespring.common.encryption.{ AESCrypto, NullCrypto }
import org.corespring.container.client.CompressedAndMinifiedComponentSets
import org.corespring.container.client.controllers.{ Assets, ComponentSets }
import org.corespring.container.client.hooks._
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.{ Organization, Standard, Subject }
import org.corespring.platform.core.models.auth.{ AccessToken, ApiClient }
import org.corespring.platform.core.models.item.{ FieldValue, Item, PlayItemTransformationCache }
import org.corespring.platform.core.services._
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.models.{ Mode, PlayerOptions }
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.auth.wired.{ ItemAuthWired, SessionAuthWired }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.permissionNotGranted
import org.corespring.v2.player.{ controllers => apiControllers, hooks => apiHooks }
import org.corespring.v2.player.permissions.SimpleWildcardChecker
import play.api.{ Configuration, Logger, Mode => PlayMode, Play }
import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }
import play.api.mvc._
import scalaz.{ Failure, Success, Validation }
import securesocial.core.{ Identity, SecureSocial }

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = V2LoggerFactory.getLogger("V2PlayerIntegration")

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
    def itemService = ItemServiceWired
    def cache = PlayItemTransformationCache
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

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = org.id -> PlayerOptions.ANYTHING

      override def orgService: OrgService = V2PlayerIntegration.this.orgService
    }

    lazy val token = new TokenOrgIdentity[(ObjectId, PlayerOptions)] {
      override def tokenService: TokenService = V2PlayerIntegration.this.tokenService

      override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = org.id -> PlayerOptions.ANYTHING

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

  lazy val requestIdentifier = new WithRequestIdentitySequence[(ObjectId, PlayerOptions)] {
    override def identifiers: Seq[OrgRequestIdentity[(ObjectId, PlayerOptions)]] = Seq(
      requestIdentifiers.clientIdAndOptsQueryString,
      requestIdentifiers.token,
      requestIdentifiers.userSession,
      requestIdentifiers.clientIdAndOptsSession)
  }

  lazy val itemAuth = new ItemAuthWired {
    override def orgService: OrganizationService = Organization

    override def itemService: ItemService = ItemServiceWired

    override def hasPermissions(itemId: String, options: PlayerOptions): Validation[V2Error, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, None, Mode.evaluate, options).fold(m => Failure(permissionNotGranted(m)), Success(_))
    }

    override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, (ObjectId, PlayerOptions)] = {
      V2PlayerIntegration.this.requestIdentifier(request)
    }
  }

  lazy val sessionAuth: SessionAuth = new SessionAuthWired {
    override def itemAuth: ItemAuth = V2PlayerIntegration.this.itemAuth

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    override def hasPermissions(itemId: String, sessionId: String, options: PlayerOptions): Validation[V2Error, Boolean] = {
      val permissionGranter = new SimpleWildcardChecker()
      permissionGranter.allow(itemId, Some(sessionId), Mode.evaluate, options).fold(m => Failure(permissionNotGranted(m)), Success(_))
    }

    override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, (ObjectId, PlayerOptions)] = {
      V2PlayerIntegration.this.requestIdentifier(request)
    }
  }

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret

  lazy val playS3 = new ConcreteS3Service(key, secret)

  override def componentSets: ComponentSets = new CompressedAndMinifiedComponentSets {

    import Play.current

    override def allComponents: Seq[Component] = V2PlayerIntegration.this.components

    override def configuration = {
      val rc = V2PlayerIntegration.this.configuration
      val c = ConfigFactory.parseString(
        s"""
             |minify: ${rc.getBoolean("components.minify").getOrElse(Play.mode == PlayMode.Prod)}
             |gzip: ${rc.getBoolean("components.gzip").getOrElse(Play.mode == PlayMode.Prod)}
           """.stripMargin)

      new Configuration(c)
    }

    override def dependencyResolver: DependencyResolver = new DependencyResolver {
      override def components: Seq[Component] = V2PlayerIntegration.this.components
    }

    override def resource(path: String): Option[String] = Play.resource(s"container-client/bower_components/$path").map { url =>
      logger.trace(s"load resource $path")
      scala.io.Source.fromInputStream(url.openStream())(scala.io.Codec.UTF8).getLines().mkString("\n")
    }

    override def loadLibrarySource(path: String): Option[String] = {
      val componentsPath = V2PlayerIntegration.this.configuration.getString("components.path").getOrElse("?")
      val fullPath = s"$componentsPath/$path"
      val file = new File(fullPath)

      if (file.exists()) {
        logger.trace(s"load file: $path")
        Some(scala.io.Source.fromFile(file)(scala.io.Codec.UTF8).getLines().mkString("\n"))
      } else {
        Some(s"console.warn('failed to log $fullPath');")
      }
    }
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
    override def standardQueryService: QueryService[Standard] = StandardQueryService

    override val fieldValueJson: JsObject = {
      val dbo = FieldValue.collection.find().toSeq.head
      import com.mongodb.util.{ JSON => MongoJson }
      import play.api.libs.json.{ Json => PlayJson }
      PlayJson.parse(MongoJson.serialize(dbo)).as[JsObject]
    }

    override val standardsTreeJson: JsArray = {
      import play.api.Play.current

      import scala.io.Codec
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = scala.io.Source.fromInputStream(is)(Codec.UTF8).getLines().mkString("\n")
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }

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

    override def getOrgIdAndOptions(header: RequestHeader): Validation[V2Error, (ObjectId, PlayerOptions)] = V2PlayerIntegration.this.requestIdentifier(header)

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

    override def itemTransformer = V2PlayerIntegration.this.itemTransformer

    override def auth: SessionAuth = V2PlayerIntegration.this.sessionAuth
  }

  override def editorHooks: EditorHooks = new apiHooks.EditorHooks {
    override implicit def ec: ExecutionContext = V2PlayerIntegration.this.ec

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = itemTransformer.transformToV2Json

    override def auth: ItemAuth = V2PlayerIntegration.this.itemAuth
  }
}
