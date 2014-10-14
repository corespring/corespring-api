package org.corespring.wiring

import common.seed.SeedDb
import org.corespring.api.v1.ItemApi
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.services.UserServiceWired
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.v2.api.Bootstrap
import org.corespring.v2.auth.identifiers.{ OrgRequestIdentity, WithRequestIdentitySequence }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.player.V2PlayerIntegration
import org.corespring.wiring.itemTransform.ItemTransformWiring
import org.corespring.wiring.itemTransform.ItemTransformWiring.UpdateItem
import play.api.{ Configuration, Logger, Mode, Play }

/**
 * The wiring together of the app. One of the few places where using `object` is acceptable.
 */
object AppWiring {

  import play.api.Play.current

  private val logger = Logger("org.corespring.AppWiring")



  lazy val v2ApiBootstrap = new Bootstrap(
    ItemServiceWired,
    Organization,
    AccessToken,
    integration.mainSessionService,
    UserServiceWired,
    integration.secureSocialService,
    integration.itemAuth,
    integration.sessionAuth,
    v2ApiRequestIdentity,
    ItemApi.get,
    Some(itemId => ItemTransformWiring.itemTransformerActor ! UpdateItem(itemId)))

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

  lazy val containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeSpecific <- current.configuration.getConfig(s"container-${Play.current.mode.toString.toLowerCase}").orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ current.configuration.getConfig("v2.auth").getOrElse(Configuration.empty)
      logger.debug(s"Container config: \n${out.underlying.root.render}")
      out
    }
  }.getOrElse(Configuration.empty)

  //TODO - there is some crossover between V2PlayerIntegration and V2ApiBootstrap - should they be merged
  lazy val integration = new V2PlayerIntegration(
    componentLoader.all,
    containerConfig,
    SeedDb.salatDb(),
    ItemTransformWiring.itemTransformer)

  /**
   * For v2 api - we move token to the top of the list as that is the most common form of authentication.
   */
  lazy val v2ApiRequestIdentity = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[OrgRequestIdentity[OrgAndOpts]] = Seq(
      integration.requestIdentifiers.token,
      integration.requestIdentifiers.clientIdAndPlayerTokenQueryString)
  }

}
