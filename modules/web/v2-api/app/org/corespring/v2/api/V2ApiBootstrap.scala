package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.drafts.item.ItemDrafts
import org.corespring.drafts.item.models.{ OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.drafts.item.services.CommitService
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.{ ItemType, Item, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.auth.ApiClientService
import org.corespring.services.{ SubjectService, StandardService, OrganizationService }
import org.corespring.services.assessment.{ AssessmentTemplateService, AssessmentService }
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{ MetadataSetService, MetadataService }
import org.corespring.v2.api.drafts.item.json.ItemDraftJson
import org.corespring.v2.api.services.{ PlayerTokenService, _ }
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.{ IdentityJson, OrgAndOpts }
import org.corespring.v2.auth.services.{ TokenService }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionService
import play.api.libs.concurrent.Akka
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._
import scalaz.Validation

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */

trait V2ApiServices {
  def orgService: OrganizationService
  def sessionService: SessionService
  def itemService: ItemService
  def itemTypes: Seq[(String, String)]
  def itemIndexService: ItemIndexService
  def itemAuth: ItemAuth[OrgAndOpts]
  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]
  def tokenService: TokenService
  def apiClientEncryptionService: ApiClientEncryptionService
  def draftsBackend: ItemDrafts
  def itemCommitService: CommitService
  def metadataService: MetadataService
  def metadataSetService: MetadataSetService
  def assessmentService: AssessmentService
  def assessmentTemplateService: AssessmentTemplateService
  def standardService: StandardService
  def subjectService: SubjectService
  def apiClientService: ApiClientService
}

class V2ApiBootstrap(
  val services: V2ApiServices,
  val headerToOrgAndOpts: RequestIdentity[OrgAndOpts],
  val sessionCreatedHandler: Option[VersionedId[ObjectId] => Unit],
  val scoreService: ScoreService,
  val playerJsUrl: String,
  val itemTransformer: ItemTransformer,
  val jsonFormatting: JsonFormatting) {

  private object ExecutionContexts {
    import play.api.Play.current
    val itemSessionApi: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.item-session-api")
  }

  private lazy val itemToSummaryData: ItemToSummaryData = new ItemToSummaryData {
    override def subjectService: SubjectService = services.subjectService

    override def standardService: StandardService = services.standardService

    override def jsonFormatting: JsonFormatting = V2ApiBootstrap.this.jsonFormatting
  }

  private lazy val itemApi = new ItemApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getSummaryData: (Item, Option[String]) => JsValue = itemToSummaryData.toSummaryData

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemAuth: ItemAuth[OrgAndOpts] = services.itemAuth

    override def itemTypes: Seq[ItemType] = ??? //ItemType

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {
      val collection = services.orgService.defaultCollection(identity.org).map(_.toString()).toSuccess(noDefaultCollection(identity.org.id))
      collection.toOption
    }

    override def itemIndexService: ItemIndexService = services.itemIndexService
    override def itemService: ItemService = services.itemService

    override def jsonFormatting: JsonFormatting = V2ApiBootstrap.this.jsonFormatting
  }

  lazy val itemSessionApi = new ItemSessionApi {

    override def scoreService: ScoreService = V2ApiBootstrap.this.scoreService

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = services.sessionAuth

    override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = sessionCreatedHandler.map(_(itemId))

    override def orgService: OrganizationService = services.orgService

    override def encryptionService: ApiClientEncryptionService = services.apiClientEncryptionService

    override def apiClientService: ApiClientService = services.apiClientService
  }

  lazy val metadataApi = new MetadataApi {

    override def metadataService: MetadataService = services.metadataService

    override def metadataSetService: MetadataSetService = services.metadataSetService

    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi

    override def getOrgAndOptions(request: RequestHeader) = headerToOrgAndOpts(request)
  }

  lazy val assessmentApi = new AssessmentApi {
    override def assessmentService: AssessmentService = services.assessmentService
    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi
    override def getOrgAndOptions(request: RequestHeader) = headerToOrgAndOpts(request)

    override def jsonFormatting: JsonFormatting = V2ApiBootstrap.this.jsonFormatting
  }

  lazy val assessmentTemplateApi = new AssessmentTemplateApi {
    override def jsonFormatting: JsonFormatting = V2ApiBootstrap.this.jsonFormatting
    override def assessmentTemplateService: AssessmentTemplateService = services.assessmentTemplateService
    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi
    override def getOrgAndOptions(request: RequestHeader) = headerToOrgAndOpts(request)
  }

  lazy val contributorApi = new FieldValuesApi {
    override implicit def ec: ExecutionContext = ExecutionContexts.itemSessionApi
    override def getOrgAndOptions(request: RequestHeader) = headerToOrgAndOpts(request)
    override def indexService: ItemIndexService = services.itemIndexService
  }

  lazy val playerTokenService = new PlayerTokenService {
    override def service: ApiClientEncryptionService = services.apiClientEncryptionService
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

  import org.corespring.v2.api.drafts.item.{ ItemDrafts => ItemDraftsController }

  lazy val itemDrafts = new ItemDraftsController {

    def orgAndOptsToOrgAndUser(o: OrgAndOpts) = OrgAndUser(
      SimpleOrg.fromOrganization(o.org),
      o.user.map(SimpleUser.fromUser))

    override def identifyUser(rh: RequestHeader): Option[OrgAndUser] =
      headerToOrgAndOpts(rh).map(o => orgAndOptsToOrgAndUser(o)).toOption

    override def drafts: ItemDrafts = services.draftsBackend

    override def itemDraftJson: ItemDraftJson = new ItemDraftJson {
      override def jsonFormatting: JsonFormatting = V2ApiBootstrap.this.jsonFormatting
    }
  }

  lazy val controllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    playerTokenApi,
    metadataApi,
    assessmentApi,
    assessmentTemplateApi,
    contributorApi,
    externalModelLaunchApi,
    utils,
    itemDrafts)

}
