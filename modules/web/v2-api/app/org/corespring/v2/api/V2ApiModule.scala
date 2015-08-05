package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.{ PlayerDefinition, ComponentType }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.services.assessment.{ AssessmentService, AssessmentTemplateService }
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.api.drafts.item.ItemDraftsModule
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.{ SessionAuth, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ Controller, RequestHeader }

import scala.concurrent.ExecutionContext
import scalaz.Validation

case class V2ApiExecutionContext(context: ExecutionContext)

trait V2ApiModule extends ItemDraftsModule {

  import com.softwaremill.macwire.MacwireMacros._

  def orgService: OrganizationService

  def itemIndexService: ItemIndexService

  def itemAuth: ItemAuth[OrgAndOpts]

  def assessmentTemplateService: AssessmentTemplateService

  def assessmentService: AssessmentService

  def componentTypes: Seq[ComponentType]

  def scoreService: ScoreService

  def itemApiExecutionContext: ItemApiExecutionContext

  def itemSessionApiExecutionContext: ItemSessionApiExecutionContext

  def v2ApiExecutionContext: V2ApiExecutionContext

  def getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]

  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]

  def apiClientEncryptionService: ApiClientEncryptionService

  def apiClientService: ApiClientService

  def sessionCreatedCallback: VersionedId[ObjectId] => Unit

  def externalModelLaunchConfig: ExternalModelLaunchConfig

  private lazy val itemApi: Controller = wire[ItemApi]

  private lazy val itemSessionApi: Controller = wire[ItemSessionApi]

  private lazy val assessmentApi: Controller = wire[AssessmentApi]

  private lazy val assessmentTemplateApi: Controller = wire[AssessmentTemplateApi]

  private lazy val externalModelLaunchApi: Controller = wire[ExternalModelLaunchApi]

  private lazy val fieldValuesApi: Controller = wire[FieldValuesApi]

  private lazy val metadataApi: Controller = wire[MetadataApi]

  private lazy val playerTokenApi: Controller = wire[PlayerTokenApi]

  private lazy val utilsApi: Controller = wire[Utils]

  lazy val v2ApiControllers: Seq[Controller] = Seq(
    itemApi,
    itemSessionApi,
    assessmentApi,
    assessmentTemplateApi,
    externalModelLaunchApi,
    fieldValuesApi,
    metadataApi,
    playerTokenApi,
    utilsApi)

}
