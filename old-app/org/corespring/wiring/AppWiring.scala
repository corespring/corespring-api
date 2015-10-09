package org.corespring.wiring

import com.mongodb.casbah.commons.MongoDBObject
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ S3Service, ConcreteS3Service }
import org.corespring.api.v1.{ CollectionApi, ItemApiStripped }
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.common.config.AppConfig
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.dev.tools.DevTools
import org.corespring.importing.{ Bootstrap => ItemImportBootstrap }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.BasicScoreService
import org.corespring.v2.api.{ V1CollectionApiProxy, V1ItemApiProxy, V2ApiBootstrap }
import org.corespring.v2.auth.identifiers.{ OrgRequestIdentity, WithRequestIdentitySequence }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.{ CDNResolver, V2PlayerBootstrap }
import org.corespring.v2.wiring.auth.RequestIdentifiers
import org.corespring.v2.wiring.services.Services
import org.corespring.web.common.views.helpers.Defaults
import org.corespring.wiring.apiTracking.ApiTracking
import org.corespring.wiring.itemTransform.ItemTransformWiring
import org.corespring.wiring.itemTransform.ItemTransformWiring.UpdateItem
import org.corespring.wiring.sessiondb.SessionServiceFactoryImpl
import play.api.libs.json.JsValue
import play.api.mvc.{ Controller, Action, AnyContent }
import play.api.{ Configuration, Logger, Mode, Play }

/**
 * The wiring together of the app. One of the few places where using `object` is acceptable.
 */
object AppWiring {

  import play.api.Play.current

  private val logger = Logger("org.corespring.wiring.AppWiring")

  private lazy val bucket = AppConfig.assetsBucket

  lazy val playS3 = CorespringS3ServiceExtended

  private lazy val v1ItemApiProxy = new V1ItemApiProxy {

    override def get: (VersionedId[ObjectId], Option[String]) => Action[AnyContent] = ItemApi.get

    override def list: (Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent] = ItemApi.list

    override def listWithColl: (ObjectId, Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent] = ItemApi.listWithColl
  }

  private lazy val v1CollectionApiProxy = new V1CollectionApiProxy {

    override def getCollection: (ObjectId) => Action[AnyContent] = CollectionApi.getCollection

    override def list: (Option[String], Option[String], String, Int, Int, Option[String]) => Action[AnyContent] = CollectionApi.list
  }

  private val scoreService = new BasicScoreService(v2PlayerBootstrap.outcomeProcessor, v2PlayerBootstrap.scoreProcessor)

  private lazy val services: Services = new Services(
    Play.current.configuration.getConfig("v2.auth.cache").getOrElse(Configuration.empty),
    Db.salatDb(),
    ItemTransformWiring.itemTransformer,
    playS3.client,
    bucket,
    SessionServiceFactoryImpl)

  private lazy val requestIdentifiers: RequestIdentifiers = new RequestIdentifiers(
    services.secureSocialService,
    services.orgService,
    services.tokenService,
    services.apiClientEncryptionService,
    DevTools.enabled)

  /**
   * For v2 api - we move token to the top of the list as that is the most common form of authentication.
   */
  private lazy val v2ApiRequestIdentity = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[OrgRequestIdentity[OrgAndOpts]] = Seq(
      requestIdentifiers.token,
      requestIdentifiers.clientIdAndPlayerTokenQueryString,
      //Add user session in as the last resort
      requestIdentifiers.userSession)
  }

  private lazy val v2ApiBootstrap = new V2ApiBootstrap(
    services,
    v2ApiRequestIdentity,
    Some((itemId: VersionedId[ObjectId]) => ItemTransformWiring.itemTransformerActor ! UpdateItem(itemId)),
    scoreService,
    org.corespring.container.client.controllers.routes.PlayerLauncher.playerJs().url,
    ItemTransformWiring.itemTransformer)

  private lazy val itemImportBootstrap = new ItemImportBootstrap(
    services.itemAuth,
    requestIdentifiers.userSession,
    services.orgService,
    AppConfig)

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.getString("components.path").toSeq

    val showNonReleasedComponents: Boolean = containerConfig.getBoolean("components.showNonReleasedComponents")
      .getOrElse {
        Play.current.mode == Mode.Dev
      }

    val out = new FileComponentLoader(path, showNonReleasedComponents)
    out.reload
    out
  }

  private lazy val containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeSpecific <- current.configuration
        .getConfig(s"container-${Play.current.mode.toString.toLowerCase}")
        .orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ current.configuration
        .getConfig("v2.auth")
        .getOrElse(Configuration.empty)
      logger.debug(s"Container config: \n${out.underlying.root.render}")
      out
    }
  }.getOrElse(Configuration.empty)

  private def getItemIdForSessionId(sessionId: String): Option[VersionedId[ObjectId]] = {

    def toVid(json: JsValue): Option[VersionedId[ObjectId]] = {
      val vidString = (json \ "itemId").as[String]
      val vid = VersionedId(vidString)
      require(vid.map { _.version.isDefined }.getOrElse(true), s"The version must be defined for an itemId: $vid, within a session: $sessionId")
      vid
    }

    val itemIdOnly = MongoDBObject("itemId" -> 1)

    try {
      val maybeDbo = services.mainSessionService
        .load(sessionId)
        .orElse {
          services.previewSessionService.load(sessionId)
        }
      maybeDbo.map { toVid }.flatten
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        None
      }
    }
  }

  private lazy val v2PlayerBootstrap = new V2PlayerBootstrap(
    componentLoader.all,
    containerConfig,
    new CDNResolver(containerConfig, Defaults.commitHashShort).resolveDomain _,
    ItemTransformWiring.itemTransformer,
    requestIdentifiers.allIdentifiers,
    services.itemAuth,
    services.sessionAuth,
    playS3,
    playS3.getClient,
    bucket,
    services.draftsBackend,
    services.orgService,
    services.colService,
    getItemIdForSessionId)

  def controllers: Seq[Controller] = v2PlayerBootstrap.controllers ++
    v2ApiBootstrap.controllers ++
    itemImportBootstrap.controllers ++
    Seq(v1CollectionApiProxy, v1ItemApiProxy)

  def validate: Either[String, Boolean] = v2PlayerBootstrap.validate

  lazy val apiTracking: ApiTracking = new ApiTracking(
    services.tokenService,
    services.apiClientService)
}