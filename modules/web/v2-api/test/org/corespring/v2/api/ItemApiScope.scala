package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.{ ComponentType, Item, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.ExecutionContext
import scalaz.{ Success, Validation }

trait ItemApiScope extends V2ApiScope with Scope with MockFactory {

  lazy val collectionId = ObjectId.get

  def orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())

  lazy val mockItemService: ItemService = mock[ItemService]

  lazy val mockScoreService: ScoreService = {
    val m = mock[ScoreService]
    m
  }

  lazy val mockItemAuth: ItemAuth[OrgAndOpts] = {
    val m = mock[ItemAuth[OrgAndOpts]]
    m
  }

  lazy val mockItemIndexService: ItemIndexService = mock[ItemIndexService]

  lazy val mockOrgService = {
    val m = mock[OrganizationService]
    m
  }

  var itemTypes = Seq.empty[ComponentType]

  lazy val mockJsonFormatting = {
    val m = mock[JsonFormatting]
    m
  }

  lazy val apiContext = ItemApiExecutionContext(ExecutionContext.Implicits.global)

  lazy val api = new ItemApi(
    mockItemService,
    mockOrgService,
    mockItemIndexService,
    mockItemAuth,
    itemTypes,
    mockScoreService,
    mockJsonFormatting,
    apiContext,
    getOrgAndOptionsFn)
}
