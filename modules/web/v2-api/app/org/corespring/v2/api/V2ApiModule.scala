package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.{ PlayerDefinition, ComponentType }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.api.drafts.item.ItemDraftsModule
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.{ SessionAuth, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait V2ApiModule extends ItemDraftsModule {

  import com.softwaremill.macwire.MacwireMacros._

  def org: OrganizationService

  def itemIndex: ItemIndexService

  def itemAuth: ItemAuth[OrgAndOpts]

  def componentTypes: Seq[ComponentType]

  def score: ScoreService

  def itemApiExecutionContext: ItemApiExecutionContext

  def itemSessionApiExecutionContext: ItemSessionApiExecutionContext

  def getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]

  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]

  def apiClientEncryption: ApiClientEncryptionService

  def apiClient: ApiClientService

  def sessionCreatedCallback: VersionedId[ObjectId] => Unit

  def itemApi = wire[ItemApi]

  def itemSessionApi = wire[ItemSessionApi]

}
