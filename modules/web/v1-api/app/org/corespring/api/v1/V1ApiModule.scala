package org.corespring.api.v1

import com.novus.salat.Context
import org.corespring.amazon.s3.S3Service
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.services._
import org.corespring.v2
import org.corespring.v2.sessiondb.SessionServices
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

case class V1ApiExecutionContext(context: ExecutionContext)

trait V1ApiModule {

  import com.softwaremill.macwire.MacwireMacros._

  def bucket: Bucket

  def v1ApiExecutionContext: V1ApiExecutionContext

  def orgService: OrganizationService

  def orgCollectionService: OrgCollectionService

  def contentCollectionService: ContentCollectionService

  def itemService: ItemService

  def jsonFormatting: JsonFormatting

  def metadataService: MetadataService

  def s3Service: S3Service

  def metadataSetService: MetadataSetService

  def itemTransformer: ItemTransformer

  def sessionServices: SessionServices

  def v2CollectionApi: v2.api.CollectionApi

  def v2FieldValuesApi: v2.api.FieldValuesApi

  def v2ItemApi: v2.api.ItemApi

  def v2OrganizationApi: v2.api.OrganizationApi

  def tokenService: AccessTokenService

  def oauthProvider: OAuthProvider

  def standardService: StandardService

  def subjectService: SubjectService

  //Not ideal to expose this but will get the ItemApi running.
  def salatItemDao: SalatVersioningDao[Item]

  def context: Context

  lazy val salatContentService = new ItemApiContentService(itemService, salatItemDao)
  lazy val itemApiItemValidation = new ItemApiItemValidation()

  lazy val v1ItemApi: Controller = wire[org.corespring.api.v1.ItemApi]
  lazy val v1FieldValuesApi: Controller = wire[org.corespring.api.v1.FieldValuesApi]
  lazy val v1CollectionApi: Controller = wire[CollectionApi]
  lazy val v1ContributorApi: Controller = wire[ContributorApi]
  lazy val v1ItemMetadataApi: Controller = wire[ItemMetadataApi]
  lazy val v1ResourceApi: Controller = wire[ResourceApi]

  lazy val v1ApiControllers: Seq[Controller] = Seq(
    v1CollectionApi,
    v1ContributorApi,
    v1ItemMetadataApi,
    v1ResourceApi,
    v1FieldValuesApi,
    v1ItemApi)
}