package org.corespring.v2.api

import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId
import org.corespring.api.v1
import org.corespring.common.encryption.AESCrypto
import org.corespring.drafts.item.ItemDrafts
import org.corespring.drafts.item.services.{CommitService, ItemDraftService}
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.encryption.{ OrgEncrypter, OrgEncryptionService }
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.services.{ PlayerTokenService, _ }
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.{ IdentityJson, OrgAndOpts }
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.concurrent.Akka
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import securesocial.core.SecureSocial

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._
import scalaz.Validation

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */

trait V2ApiServices {
  def orgService:OrgService
  def sessionService:MongoService
  def itemService:ItemService
  def itemAuth:ItemAuth[OrgAndOpts]
  def sessionAuth:SessionAuth[OrgAndOpts,PlayerDefinition]
  def tokenService:TokenService
  def orgEncryptionService:OrgEncryptionService
  def draftService:ItemDraftService
  def itemCommitService:CommitService
}

class V2ApiBootstrap(
  val services : V2ApiServices,
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

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {
      val collection = services.orgService.defaultCollection(identity.org).map(_.toString()).toSuccess(noDefaultCollection(identity.org.id))
      collection.toOption
    }

    override def itemService: ItemService = services.itemService
  }

  lazy val itemSessionApi = new ItemSessionApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = services.sessionAuth

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

    override def orgEncryptionService: OrgEncryptionService = services.orgEncryptionService
  }

  lazy val cms = new Cms {
    override def itemTransformer: ItemTransformer = V2ApiBootstrap.this.itemTransformer

    override def itemService: ItemService = services.itemService

    override def v1ApiCreate = (request) => { org.corespring.api.v1.ItemApi.create()(request) }

    override def draftService: ItemDraftService = services.draftService

    override def identifyUser(rh: RequestHeader): Option[User] = {
      SecureSocial.currentUser(rh).flatMap(identity => User.getUser(identity.identityId))
    }
  }

  import org.corespring.v2.api.drafts.item.{ItemDrafts => ItemDraftsController}

  lazy val itemDrafts = new ItemDraftsController{
    override def drafts: ItemDrafts = new ItemDrafts {
      override def itemService: ItemService = services.itemService

      override def draftService: ItemDraftService = services.draftService

      override def commitService: CommitService = services.itemCommitService
    }

    override def authenticateUser(rh: RequestHeader): Option[User] = {
      SecureSocial.currentUser(rh).flatMap(identity => User.getUser(identity.identityId))
    }
  }

  lazy val controllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    playerTokenApi,
    externalModelLaunchApi,
    utils,
    cms,
    itemDrafts)

}
