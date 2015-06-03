package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.drafts.item.ItemDrafts
import org.corespring.drafts.item.models.{OrgAndUser, SimpleOrg, SimpleUser}
import org.corespring.drafts.item.services.CommitService
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.{ItemType, Item, PlayerDefinition}
import org.corespring.platform.core.encryption.{ApiClientEncryptionService, ApiClientEncrypter}
import org.corespring.platform.core.services.item.{ItemIndexService, ItemService}
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.services.{PlayerTokenService, _}
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.{IdentityJson, OrgAndOpts}
import org.corespring.v2.auth.services.{OrgService, TokenService}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._
import scalaz.Validation

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */

trait V2ApiServices {
  def orgService: OrgService
  def sessionService: MongoService
  def itemService: ItemService
  def itemType: ItemType
  def itemIndexService: ItemIndexService
  def itemAuth: ItemAuth[OrgAndOpts]
  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]
  def tokenService: TokenService
  def apiClientEncryptionService: ApiClientEncryptionService
  def draftsBackend: ItemDrafts
  def itemCommitService: CommitService
}

class V2ApiBootstrap(
  val services: V2ApiServices,
  val headerToOrgAndOpts: RequestIdentity[OrgAndOpts],
  val sessionCreatedHandler: Option[VersionedId[ObjectId] => Unit],
  val scoreService: ScoreService,
  val playerJsUrl: String,
  val itemTransformer: ItemTransformer) {

  private object ExecutionContexts {
    import play.api.Play.current
    val itemSessionApi: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")
  }

  private lazy val itemApi = new ItemApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getSummaryData: (Item, Option[String]) => JsValue = new ItemToSummaryData {}.toSummaryData

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemAuth: ItemAuth[OrgAndOpts] = services.itemAuth

    override def itemType: ItemType = ItemType

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {
      val collection = services.orgService.defaultCollection(identity.org).map(_.toString()).toSuccess(noDefaultCollection(identity.org.id))
      collection.toOption
    }

    override def itemIndexService: ItemIndexService = services.itemIndexService
    override def itemService: ItemService = services.itemService
  }

  lazy val itemSessionApi = new ItemSessionApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = services.sessionAuth

    override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = sessionCreatedHandler.map(_(itemId))

    override def orgService: OrgService = services.orgService
  }

  lazy val playerTokenService = new PlayerTokenService {
    override def encrypter: ApiClientEncrypter = new ApiClientEncrypter(AESCrypto)
  }

  lazy val playerTokenApi = new PlayerTokenApi {

    override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override def tokenService: PlayerTokenService = V2ApiBootstrap.this.playerTokenService
  }

  lazy val v2SessionService = new V2SessionService {

    override def createExternalModelSession(orgAndOpts: OrgAndOpts, model: JsObject): Option[ObjectId] = {
      services.sessionService.create(
        Json.obj(
          "identity" -> IdentityJson(orgAndOpts),
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

    override def tokenService: TokenService = services.tokenService

    override def apiClientEncryptionService: ApiClientEncryptionService = services.apiClientEncryptionService
  }


  import org.corespring.v2.api.drafts.item.{ItemDrafts => ItemDraftsController}

  lazy val itemDrafts = new ItemDraftsController {

    def orgAndOptsToOrgAndUser(o: OrgAndOpts) = OrgAndUser(
      SimpleOrg.fromOrganization(o.org),
      o.user.map(SimpleUser.fromUser))

    override def identifyUser(rh: RequestHeader): Option[OrgAndUser] =
      headerToOrgAndOpts(rh).map(o => orgAndOptsToOrgAndUser(o)).toOption

    override def drafts: ItemDrafts = services.draftsBackend
  }

  lazy val controllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    playerTokenApi,
    externalModelLaunchApi,
    utils,
    itemDrafts)

}
