package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.encryption.{ OrgEncrypter, OrgEncryptionService }
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.{ PlayerTokenService, _ }
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.concurrent.Akka
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._
import scalaz.Validation

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */
class V2ApiBootstrap(
  val orgService: OrgService,
  val sessionService: MongoService,
  val itemAuth: ItemAuth[OrgAndOpts],
  val itemService: ItemService,
  val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  val headerToOrgAndOpts: RequestIdentity[OrgAndOpts],
  val sessionCreatedHandler: Option[VersionedId[ObjectId] => Unit],
  val scoreService: ScoreService,
  val playerJsUrl: String,
  val tokenService: TokenService,
  val orgEncryptionService: OrgEncryptionService) {

  private object ExecutionContexts {
    import play.api.Play.current
    val itemSessionApi: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")

  }

  private lazy val itemApi = new ItemApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService
    private lazy val itemTransformer = new ItemTransformerToSummaryData {}

    override def transform: (Item, Option[String]) => JsValue = itemTransformer.transform

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemAuth: ItemAuth[OrgAndOpts] = V2ApiBootstrap.this.itemAuth

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {
      val collection = orgService.defaultCollection(identity.org).map(_.toString()).toSuccess(noDefaultCollection(identity.org.id))
      collection.toOption
    }

    override def itemService: ItemService = V2ApiBootstrap.this.itemService
  }

  lazy val itemSessionApi = new ItemSessionApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = V2ApiBootstrap.this.sessionAuth

    override def sessionService = V2ApiBootstrap.this.sessionService

    override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = sessionCreatedHandler.map(_(itemId))
  }

  lazy val playerTokenService = new PlayerTokenService {
    override def encrypter: OrgEncrypter = new OrgEncrypter(AESCrypto)
  }

  lazy val playerTokenApi = new PlayerTokenApi {

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override def tokenService: PlayerTokenService = V2ApiBootstrap.this.playerTokenService
  }

  lazy val v2SessionService = new V2SessionService {

    override def createExternalModelSession(orgId: ObjectId, model: JsObject): Option[ObjectId] = {
      sessionService.create(
        Json.obj(
          "orgId" -> orgId.toString,
          "item" -> model))
    }
  }

  lazy val externalModelLaunchApi = new ExternalModelLaunchApi {
    override def sessionService: V2SessionService = V2ApiBootstrap.this.v2SessionService

    override def tokenService: PlayerTokenService = V2ApiBootstrap.this.playerTokenService

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def playerJsUrl: String = V2ApiBootstrap.this.playerJsUrl
    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)
  }

  lazy val utils = new Utils {
    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def tokenService: TokenService = V2ApiBootstrap.this.tokenService

    override def orgEncryptionService: OrgEncryptionService = V2ApiBootstrap.this.orgEncryptionService
  }

  lazy val controllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    playerTokenApi,
    externalModelLaunchApi,
    utils)

}