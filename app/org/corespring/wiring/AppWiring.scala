package org.corespring.wiring

import common.db.Db
import org.bson.types.ObjectId
import org.corespring.api.v1.{ CollectionApi, ItemApi }
import org.corespring.common.config.AppConfig
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.importing.{ Bootstrap => ItemImportBootstrap }
import org.corespring.platform.core.models.auth.{ApiClientService, ApiClient}
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.BasicScoreService
import org.corespring.v2.api.{ V1CollectionApiProxy, V1ItemApiProxy, V2ApiBootstrap }
import org.corespring.v2.auth.identifiers.{ApiClientRequestIdentity, OrgRequestIdentity, WithRequestIdentitySequence}
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.{ CDNResolver, V2PlayerBootstrap }
import org.corespring.v2.wiring.auth.{ApiClientRequestIdentifiers, RequestIdentifiers}
import org.corespring.v2.wiring.services.Services
import org.corespring.web.common.views.helpers.Defaults
import org.corespring.wiring.apiTracking.ApiTracking
import org.corespring.wiring.itemTransform.ItemTransformWiring
import org.corespring.wiring.itemTransform.ItemTransformWiring.UpdateItem
import play.api.mvc.{RequestHeader, Controller, Action, AnyContent}
import play.api.{ Configuration, Logger, Mode, Play }

import scalaz.Validation

/**
 * The wiring together of the app. One of the few places where using `object` is acceptable.
 */
object AppWiring {

  import play.api.Play.current

  private val logger = Logger("org.corespring.AppWiring")

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
    ItemTransformWiring.itemTransformer)

  private lazy val requestIdentifiers: RequestIdentifiers = new RequestIdentifiers(
    services.secureSocialService,
    services.orgService,
    services.tokenService,
    services.orgEncryptionService,
    Play.current.configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false))

  private lazy val apiClientRequestIdentifiers: ApiClientRequestIdentifiers = new ApiClientRequestIdentifiers(
    services.secureSocialService,
    services.apiClientService,
    services.orgService,
    services.tokenService,
    services.orgEncryptionService,
    Play.current.configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false))

  private lazy val v2ApiBootstrap = new V2ApiBootstrap(
    services.orgService,
    services.mainSessionService,
    services.itemAuth,
    ItemServiceWired,
    services.sessionAuth,
    v2ApiRequestIdentity,
    v2ApiApiClientRequestIdentity,
    Some((itemId: VersionedId[ObjectId]) => ItemTransformWiring.itemTransformerActor ! UpdateItem(itemId)),
    scoreService,
    org.corespring.container.client.controllers.routes.PlayerLauncher.playerJs().url,
    services.tokenService,
    services.orgEncryptionService)

  private lazy val itemImportBootstrap = new ItemImportBootstrap(
    services.itemAuth,
    requestIdentifiers.userSession,
    services.orgService,
    AppConfig)

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.getString("components.path").toSeq

    val showReleasedOnlyComponents: Boolean = containerConfig.getBoolean("components.showReleasedOnly")
      .getOrElse {
        Play.current.mode == Mode.Prod
      }

    val out = new FileComponentLoader(path, showReleasedOnlyComponents)
    out.reload
    out
  }

  private lazy val containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeSpecific <- current.configuration.getConfig(s"container-${Play.current.mode.toString.toLowerCase}").orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ current.configuration.getConfig("v2.auth").getOrElse(Configuration.empty)
      logger.debug(s"Container config: \n${out.underlying.root.render}")
      out
    }
  }.getOrElse(Configuration.empty)

  private lazy val v2PlayerBootstrap = new V2PlayerBootstrap(
    componentLoader.all,
    containerConfig,
    services.mainSessionService,
    services.previewSessionService,
    ItemTransformWiring.itemTransformer,
    services.secureSocialService,
    requestIdentifiers.allIdentifiers,
    services.itemAuth,
    services.sessionAuth,
    new CDNResolver(containerConfig, Defaults.commitHashShort))

  /**
   * For v2 api - we move token to the top of the list as that is the most common form of authentication.
   */
  private lazy val v2ApiRequestIdentity = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[OrgRequestIdentity[OrgAndOpts]] = Seq(
      requestIdentifiers.token,
      requestIdentifiers.clientIdAndPlayerTokenQueryString)
  }

  private lazy val v2ApiApiClientRequestIdentity = new WithRequestIdentitySequence[ApiClient] {
    override def identifiers: Seq[ApiClientRequestIdentity[ApiClient]] = Seq(
      apiClientRequestIdentifiers.token,
      apiClientRequestIdentifiers.clientIdAndPlayerTokenQueryString
    )
  }

  def controllers: Seq[Controller] = v2PlayerBootstrap.controllers ++
    v2ApiBootstrap.controllers ++
    itemImportBootstrap.controllers ++
    Seq(v1CollectionApiProxy, v1ItemApiProxy)

  def validate: Either[String, Boolean] = v2PlayerBootstrap.validate

  lazy val apiTracking: ApiTracking = new ApiTracking(
    services.tokenService,
    services.apiClientService)
}
