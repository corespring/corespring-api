package org.corespring.services.salat.bootstrap

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models._
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.models.assessment.{ Assessment, AssessmentTemplate }
import org.corespring.models.auth.{ ApiClient, AccessToken, Permission }
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.registration.RegistrationToken
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.services.salat.registration.RegistrationTokenService
import org.corespring.services.salat.item.{ FieldValueService, ItemAssetService, ItemService }
import org.corespring.services.salat._
import org.corespring.services.salat.assessment.{ AssessmentService, AssessmentTemplateService }
import org.corespring.services.salat.auth.{ ApiClientService, AccessTokenService }
import org.corespring.services.salat.metadata.{ MetadataSetService, MetadataService }
import org.corespring.{ services => interface }

trait SalatServices extends interface.bootstrap.Services {

  def db: MongoDB
  implicit def context: Context
  def bucket: Bucket
  def archiveConfig: ArchiveConfig
  def accessTokenConfig: AccessTokenConfig
  def s3: AmazonS3

  private val logger = Logger(classOf[SalatServices])

  RegisterJodaTimeConversionHelpers()

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

    logger.debug(s"function=initArchive org=${archiveOrg} - inserting")
    orgService.insert(archiveOrg, None) match {
      case Left(e) => throw new RuntimeException("Failed to Bootstrap - error inserting archive org")

      case Right(_) => {
        logger.debug(s"function=initArchive collection=${coll} - inserting")
        contentCollectionService.insertCollection(archiveOrg.id, coll, Permission.Write) match {
          case Left(e) => throw new RuntimeException("Failed to Bootstrap - error inserting archive org")
          case _ => logger.info("Archive org and content collection initialised")
        }
      }
    }
  }

  initArchive()

  import com.softwaremill.macwire.MacwireMacros._

  lazy val contentCollectionDao = new SalatDAO[ContentCollection, ObjectId](db("contentcolls")) {}

  lazy val orgDao = new SalatDAO[Organization, ObjectId](db("orgs")) {}

  lazy val tokenDao = new SalatDAO[AccessToken, ObjectId](db("accessTokens")) {}

  lazy val assessmentTemplateDao = new SalatDAO[AssessmentTemplate, ObjectId](db("content")) {}
  lazy val assessmentsDao = new SalatDAO[Assessment, ObjectId](db("assessments")) {}
  lazy val apiClientDao = new SalatDAO[ApiClient, ObjectId](db("apiClients")) {}
  lazy val userDao = new SalatDAO[User, ObjectId](db("users")) {}
  lazy val metadataSetDao = new SalatDAO[MetadataSet, ObjectId](db("metadataSets")) {}
  lazy val registrationTokenDao = new SalatDAO[RegistrationToken, ObjectId](db("regTokens")) {}
  lazy val standardDao = new SalatDAO[Standard, ObjectId](db("standards")) {}
  lazy val subjectDao = new SalatDAO[Subject, ObjectId](db("subjects")) {}
  lazy val fieldValueDao = new SalatDAO[FieldValue, ObjectId](db("fieldValues")) {}

  lazy val itemDao = new SalatVersioningDao[Item] {

    def db: MongoDB = SalatServices.this.db

    protected def collectionName: String = "content"

    protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

    protected implicit def context: Context = SalatServices.this.context

    override def checkCurrentCollectionIntegrity: Boolean = false
  }

  lazy val copyFiles: (String, String) => Unit = s3.copyObject(bucket.bucket, _, bucket.bucket, _)
  lazy val deleteFiles: (String) => Unit = s3.deleteObject(bucket.bucket, _)

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
  override lazy val contentCollectionService = new ContentCollectionService(contentCollectionDao, context, orgService, itemService)

  override lazy val orgService: interface.OrganizationService = new OrganizationService(orgDao, context, contentCollectionService, metadataSetService, itemService)

  override lazy val tokenService: interface.auth.AccessTokenService = wire[AccessTokenService]

  override lazy val assessmentTemplateService: interface.assessment.AssessmentTemplateService = wire[AssessmentTemplateService]

  override lazy val metadataService: interface.metadata.MetadataService = wire[MetadataService]

  override lazy val apiClientService: interface.auth.ApiClientService = wire[ApiClientService]

  override lazy val userService: interface.UserService = wire[UserService]

  override lazy val registrationTokenService: interface.RegistrationTokenService = wire[RegistrationTokenService]

  override lazy val metadataSetService: interface.metadata.MetadataSetService = new MetadataSetService(metadataSetDao, context, orgService)

  override lazy val itemService: interface.item.ItemService = new ItemService(itemDao, itemAssetService, contentCollectionService, context, archiveConfig)

  override lazy val assessmentService: interface.assessment.AssessmentService = wire[AssessmentService]

  override lazy val subjectService: interface.SubjectService = wire[SubjectService]

  override lazy val standardService: interface.StandardService = wire[StandardService]

  override lazy val fieldValueService: interface.item.FieldValueService = wire[FieldValueService]
}
