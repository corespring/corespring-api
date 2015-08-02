package org.corespring.ap.v1

import org.corespring.amazon.s3.S3Service
import org.corespring.api.v1.{ ResourceApi, ItemMetadataApi, ContributorApi, CollectionApi }
import org.corespring.models.json.JsonFormatting
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{ MetadataSetService, MetadataService }
import org.corespring.services.{ ContentCollectionService, OrganizationService }
import org.corespring.v2.sessiondb.SessionServices
import play.api.mvc.Controller

trait V1ApiModule {

  import com.softwaremill.macwire.MacwireMacros._

  def orgService: OrganizationService

  def contentCollectionService: ContentCollectionService

  def itemService: ItemService

  def jsonFormatting: JsonFormatting

  def metadataService: MetadataService

  def s3Service: S3Service

  def metadataSetService: MetadataSetService

  def itemTransformer: ItemTransformer

  def sessionServices: SessionServices

  lazy val v1CollectionApi: Controller = wire[CollectionApi]

  lazy val v1ContributorApi: Controller = wire[ContributorApi]

  lazy val v1ItemMetadataApi: Controller = wire[ItemMetadataApi]

  lazy val v1ResourceApi: Controller = wire[ResourceApi]

  lazy val v1ApiControllers: Seq[Controller] = Seq(
    v1CollectionApi,
    v1ContributorApi,
    v1ItemMetadataApi,
    v1ResourceApi)
}