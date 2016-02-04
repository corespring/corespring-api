package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.{ PlayerDefinition, ComponentType }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.drafts.item.ItemDraftsModule
import org.corespring.v2.api.services.{ PlayerTokenService, ScoreService }
import org.corespring.v2.auth.{ SessionAuth, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.{ SessionService, SessionServices }
import play.api.mvc.{ Controller, RequestHeader }

import scala.concurrent.ExecutionContext
import scalaz.Validation

case class V2ApiExecutionContext(context: ExecutionContext)

trait V2ApiModule
  extends ItemDraftsModule
  with org.corespring.services.bootstrap.Services {

  import com.softwaremill.macwire.MacwireMacros._

  def defaultOrgs: DefaultOrgs

  def mainSessionService: SessionService

  def itemIndexService: ItemIndexService

  def itemAuth: ItemAuth[OrgAndOpts]

  def componentTypes: Seq[ComponentType]

  def scoreService: ScoreService

  def itemApiExecutionContext: ItemApiExecutionContext

  def itemSessionApiExecutionContext: ItemSessionApiExecutionContext

  def v2ApiExecutionContext: V2ApiExecutionContext

  def getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]

  def getOrgOptsAndApiClientFn: RequestHeader => Validation[V2Error, (OrgAndOpts, ApiClient)]

  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]

  def apiClientEncryptionService: ApiClientEncryptionService

  def sessionCreatedCallback: VersionedId[ObjectId] => Unit

  def externalModelLaunchConfig: ExternalModelLaunchConfig

  def sessionServices: SessionServices

  lazy val playerTokenService: PlayerTokenService = wire[PlayerTokenService]

  private lazy val itemApi: Controller = wire[ItemApi]

  private lazy val itemSessionApi: Controller = wire[ItemSessionApi]

  private lazy val assessmentApi: Controller = wire[AssessmentApi]

  private lazy val assessmentTemplateApi: Controller = wire[AssessmentTemplateApi]

  private lazy val externalModelLaunchApi: Controller = wire[ExternalModelLaunchApi]

  private lazy val fieldValuesApi: Controller = wire[FieldValuesApi]

  private lazy val metadataApi: Controller = wire[MetadataApi]

  private lazy val playerTokenApi: Controller = wire[PlayerTokenApi]

  private lazy val utilsApi: Controller = wire[Utils]

  private lazy val collectionApi: Controller = wire[CollectionApi]

  private lazy val organizationApi: Controller = wire[OrganizationApi]

  //Expose this api so v1 api can use it
  lazy val v2ItemApi: ItemApi = itemApi.asInstanceOf[ItemApi]
  lazy val v2CollectionApi: CollectionApi = collectionApi.asInstanceOf[CollectionApi]
  lazy val v2FieldValuesApi: FieldValuesApi = fieldValuesApi.asInstanceOf[FieldValuesApi]
  lazy val v2OrganizationApi: OrganizationApi = organizationApi.asInstanceOf[OrganizationApi]

  lazy val v2ApiControllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    assessmentApi,
    assessmentTemplateApi,
    externalModelLaunchApi,
    fieldValuesApi,
    metadataApi,
    playerTokenApi,
    utilsApi,
    collectionApi,
    organizationApi)

}
