package org.corespring.services.salat.bootstrap

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models._
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.models.assessment.{ Assessment, AssessmentTemplate }
import org.corespring.models.auth.{ AccessToken, ApiClient }
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.registration.RegistrationToken
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat._
import org.corespring.services.salat.assessment.{ AssessmentService, AssessmentTemplateService }
import org.corespring.services.salat.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.salat.item.{ FieldValueService, ItemAggregationService, ItemAssetService, ItemService }
import org.corespring.services.salat.metadata.{ MetadataService, MetadataSetService }
import org.corespring.services.salat.registration.RegistrationTokenService
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success }

object CollectionNames {

  val accessToken = "accessTokens"
  val apiClient = "apiClients"
  val assessment = "assessments"
  val assessmentTemplate = "content"
  val contentCollection = "contentcolls"
  val fieldValue = "fieldValues"
  val item = "content"
  val metadataSet = "metadataSets"
  val organization = "orgs"
  val registrationToken = "regTokens"
  val standard = "ccstandards"
  val subject = "subjects"
  val user = "users"
  val versionedItem = "versioned_content"

  val all = Seq(
    accessToken,
    apiClient,
    assessment,
    assessmentTemplate,
    contentCollection,
    fieldValue,
    item,
    metadataSet,
    organization,
    registrationToken,
    standard,
    subject,
    user,
    versionedItem)
}

case class SalatServicesExecutionContext(ctx: ExecutionContext)

trait SalatServices extends interface.bootstrap.Services {

  def salatServicesExecutionContext: SalatServicesExecutionContext
  def db: MongoDB
  implicit def context: Context
  def bucket: Bucket
  def archiveConfig: ArchiveConfig
  def accessTokenConfig: AccessTokenConfig
  def s3: AmazonS3

  def mostRecentDateModifiedForSessions: Seq[ObjectId] => Option[DateTime]

  private val logger = Logger(classOf[SalatServices])

  RegisterJodaTimeConversionHelpers()

  def init = {

    logger.debug(s"function=init - check to see if it's already inited")
    orgService.findOneById(archiveConfig.orgId).getOrElse {
      logger.debug(s"function=init - call initArchive")
      initArchive()
    }

    initIndexes()
  }

  private def initIndexes() = {
    logger.debug(s"initing indexes")
    //Item
    db(CollectionNames.item).ensureIndex("collectionId")
    db(CollectionNames.item).ensureIndex(MongoDBObject("_id._id" -> 1, "_id.version" -> 1))
  }

  /**
   * Init the archive org and contentcollection
   */
  private def initArchive(): Unit = {

    val archiveOrg = Organization(
      name = "archive-org",
      id = archiveConfig.orgId)

    val coll = ContentCollection(
      "archive-collection",
      id = archiveConfig.contentCollectionId,
      ownerOrgId = archiveOrg.id)

    logger.debug(s"function=initArchive org=$archiveOrg - inserting")

    orgService.insert(archiveOrg, None) match {
      case Failure(e) => throw new RuntimeException(s"Failed to Bootstrap - error inserting archive org: $e")
      case Success(_) =>
        logger.debug(s"function=initArchive collection=$coll - inserting")
        contentCollectionService.insertCollection(coll) match {
          case Failure(e) => throw new RuntimeException(s"Failed to Bootstrap - error inserting archive org: $e")
          case _ => logger.info("Archive org and content collection initialised")
        }

    }
  }

  init

  import com.softwaremill.macwire.MacwireMacros._

  lazy val apiClientDao = new SalatDAO[ApiClient, ObjectId](db(CollectionNames.apiClient)) {}
  lazy val assessmentsDao = new SalatDAO[Assessment, ObjectId](db(CollectionNames.assessment)) {}
  lazy val assessmentTemplateDao = new SalatDAO[AssessmentTemplate, ObjectId](db(CollectionNames.item)) {}
  lazy val contentCollectionDao = new SalatDAO[ContentCollection, ObjectId](db(CollectionNames.contentCollection)) {}
  lazy val fieldValueDao = new SalatDAO[FieldValue, ObjectId](db(CollectionNames.fieldValue)) {}
  lazy val metadataSetDao = new SalatDAO[MetadataSet, ObjectId](db(CollectionNames.metadataSet)) {}
  lazy val orgDao = new SalatDAO[Organization, ObjectId](db(CollectionNames.organization)) {}
  lazy val registrationTokenDao = new SalatDAO[RegistrationToken, ObjectId](db(CollectionNames.registrationToken)) {}
  lazy val standardDao = new SalatDAO[Standard, ObjectId](db(CollectionNames.standard)) {}
  lazy val subjectDao = new SalatDAO[Subject, ObjectId](db(CollectionNames.subject)) {}
  lazy val tokenDao = new SalatDAO[AccessToken, ObjectId](db(CollectionNames.accessToken)) {}
  lazy val userDao = new SalatDAO[User, ObjectId](db(CollectionNames.user)) {}

  def itemDao: VersioningDao[Item, VersionedId[ObjectId]] = {
    salatItemDao
  }

  lazy val salatItemDao = new SalatVersioningDao[Item] {

    def db: MongoDB = SalatServices.this.db

    protected def collectionName: String = CollectionNames.item

    protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

    protected implicit def context: Context = SalatServices.this.context

    override def checkCurrentCollectionIntegrity: Boolean = false
  }

  lazy val itemAssetService: interface.item.ItemAssetService = new ItemAssetService(
    s3.copyObject(bucket.bucket, _, bucket.bucket, _),
    s3.deleteObject(bucket.bucket, _))

  /**
   * Note:
   * There are some circular dependencies, which require call-by-name.
   * Later versions of macwire support this but not the version for 2.10:
   * see: https://github.com/adamw/macwire/pull/29
   *
   * For now going to manually build the objects
   */

  override lazy val apiClientService: interface.auth.ApiClientService = wire[ApiClientService]
  override lazy val assessmentService: interface.assessment.AssessmentService = wire[AssessmentService]
  override lazy val assessmentTemplateService: interface.assessment.AssessmentTemplateService = wire[AssessmentTemplateService]
  override lazy val contentCollectionService = new ContentCollectionService(contentCollectionDao, context, orgCollectionService, shareItemWithCollectionsService, itemService, archiveConfig)
  override lazy val fieldValueService: interface.item.FieldValueService = wire[FieldValueService]
  override lazy val itemAggregationService: interface.item.ItemAggregationService = new ItemAggregationService(db(CollectionNames.item), salatServicesExecutionContext)
  override lazy val itemService: interface.item.ItemService = new ItemService(
    itemDao,
    db(CollectionNames.item),
    itemAssetService,
    orgCollectionService,
    context,
    archiveConfig,
    salatServicesExecutionContext)
  override lazy val metadataService: interface.metadata.MetadataService = wire[MetadataService]
  override lazy val metadataSetService: interface.metadata.MetadataSetService = new MetadataSetService(metadataSetDao, context, orgService)
  override lazy val orgCollectionService: interface.OrgCollectionService = new OrgCollectionService(
    orgService,
    contentCollectionService,
    itemService,
    orgDao,
    contentCollectionDao,
    salatServicesExecutionContext,
    context)
  override lazy val orgService: interface.OrganizationService = new OrganizationService(orgDao, context, orgCollectionService, contentCollectionService, metadataSetService, itemService)
  override lazy val registrationTokenService: interface.RegistrationTokenService = wire[RegistrationTokenService]
  override lazy val shareItemWithCollectionsService: interface.ShareItemWithCollectionsService = new ShareItemWithCollectionsService(itemDao, itemService, orgCollectionService)
  override lazy val standardService: interface.StandardService = wire[StandardService]
  override lazy val subjectService: interface.SubjectService = wire[SubjectService]
  override lazy val tokenService: interface.auth.AccessTokenService = wire[AccessTokenService]
  override lazy val userService: interface.UserService = wire[UserService]
  override lazy val cloneItemService: interface.CloneItemService = wire[CloneItemService]
}
