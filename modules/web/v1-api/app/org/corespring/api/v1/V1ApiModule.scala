package org.corespring.ap.v1

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.api.v1._
import org.corespring.models.item.Item
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.auth.{AccessTokenService, ApiClientService}
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.{ MetadataSetService, MetadataService }
import org.corespring.services.{UserService, ContentCollectionService, OrganizationService}
import org.corespring.v2.sessiondb.SessionServices
import play.api.mvc.Controller
import org.corespring.v2
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

  def v2CollectionApi: v2.api.CollectionApi

  def v2FieldValuesApi: v2.api.FieldValuesApi

  def v2ItemApi: v2.api.ItemApi

  def apiClientService:ApiClientService

  def accessTokenService: AccessTokenService

  def userService:UserService

  lazy val salatContentService = //new SalatContentService[Item,VersionedId[ObjectId]] {

  //}
  lazy val oauthProvider = wire[OAuthProvider]

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