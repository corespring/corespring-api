package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.{ OrgEncryptionService, OrgEncrypter }
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.{ AccessToken, AccessTokenService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services._
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */
class Bootstrap(
  val itemService: ItemService,
  val v1OrgService: OrganizationService,
  val accessTokenService: AccessTokenService,
  val sessionService: MongoService,
  val userService: UserService,
  val secureSocialService: SecureSocialService,
  val itemAuth: ItemAuth[OrgAndOpts],
  val sessionAuth: SessionAuth[OrgAndOpts],
  val headerToOrgAndOpts: RequestIdentity[OrgAndOpts],
  val sessionCreatedHandler: Option[VersionedId[ObjectId] => Unit],
  val outcomeProcessor: OutcomeProcessor,
  val scoreProcessor: ScoreProcessor,
  val v1ItemApiProxy: V1ItemApiProxy,
  val v1CollectionApiProxy: V1CollectionApiProxy,
  val tokenService: TokenService,
  val orgEncryptionService: OrgEncryptionService) {

  private val scoreService = new BasicScoreService(outcomeProcessor, scoreProcessor)

  protected val orgService: OrgService = new OrgService {
    override def defaultCollection(o: Organization): Option[ObjectId] = {
      v1OrgService.getDefaultCollection(o.id) match {
        case Left(e) => None
        case Right(c) => Some(c.id)
      }
    }

    override def org(id: ObjectId): Option[Organization] = v1OrgService.findOneById(id)
  }

  protected val itemPermissionService: PermissionService[Organization, Item] = new ItemPermissionService {
    override def organizationService: OrganizationService = Bootstrap.this.v1OrgService
  }

  protected val sessionPermissionService: PermissionService[Organization, JsValue] = new SessionPermissionService {

  }

  private object ExecutionContexts {
    import play.api.Play.current
    val itemSessionApi: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")

  }

  private lazy val itemApi = new ItemApi {

    override def scoreService: ScoreService = Bootstrap.this.scoreService
    private lazy val itemTransformer = new ItemTransformerToSummaryData {}

    override def transform: (Item, Option[String]) => JsValue = itemTransformer.transform

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemService: ItemService = Bootstrap.this.itemService

    override def itemAuth: ItemAuth[OrgAndOpts] = Bootstrap.this.itemAuth

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {
      val collection = orgService.defaultCollection(identity.org).map(_.toString()).toSuccess(noDefaultCollection(identity.org.id))
      collection.toOption
    }
  }

  lazy val itemSessionApi = new ItemSessionApi {

    override def scoreService: ScoreService = Bootstrap.this.scoreService

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def sessionAuth: SessionAuth[OrgAndOpts] = Bootstrap.this.sessionAuth

    override def sessionService = Bootstrap.this.sessionService

    override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = sessionCreatedHandler.map(_(itemId))
  }

  lazy val playerTokenApi = new PlayerTokenApi {
    override def encrypter: OrgEncrypter = new OrgEncrypter(AESCrypto)

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)
  }

  lazy val utils = new Utils {
    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def tokenService: TokenService = Bootstrap.this.tokenService

    override def orgEncryptionService: OrgEncryptionService = Bootstrap.this.orgEncryptionService
  }

  lazy val controllers: Seq[Controller] = Seq(itemApi, itemSessionApi, playerTokenApi, v1ItemApiProxy, v1CollectionApiProxy, utils)
}
