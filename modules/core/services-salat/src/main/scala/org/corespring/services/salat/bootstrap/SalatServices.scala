package org.corespring.services.salat.bootstrap

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models._
import org.corespring.models.assessment.{ Assessment, AssessmentTemplate }
import org.corespring.models.auth.{ ApiClient, AccessToken, Permission }
import org.corespring.models.item.Item
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.registration.RegistrationToken
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.services.salat.registration.RegistrationTokenService
import org.corespring.services.{SubjectService, StandardService}
import org.corespring.services.item.FieldValueService
import org.corespring.services.salat.item.{ ItemAssetService, ItemService }
import org.corespring.services.salat.{ OrganizationService }
import org.corespring.services.salat.{ UserService, ContentCollectionService }
import org.corespring.services.salat.assessment.{ AssessmentService, AssessmentTemplateService }
import org.corespring.services.salat.auth.{ ApiClientService, AccessTokenService }
import org.corespring.services.salat.metadata.{ MetadataSetService, MetadataService }
import org.corespring.{ services => interface }

case class AwsConfig(key: String, secret: String, bucket: String)
case class ArchiveConfig(contentCollectionId: ObjectId, orgId: ObjectId)

case class AppMode(mode: String) {
  def isProd = mode == "prod"
}

case class AccessTokenConfig(tokenDurationInHours: Int = 24)

trait SalatServices extends interface.bootstrap.Services {

  def db: MongoDB
  implicit def context: Context
  def aws: AwsConfig
  def archiveConfig: ArchiveConfig
  def accessTokenConfig: AccessTokenConfig
  def s3: AmazonS3
  @deprecated("This is a legacy function - remove", "1.0")
  def mode: AppMode

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

    org.insert(archiveOrg, None) match {
      case Left(e) => throw new RuntimeException("Failed to Bootstrap - error inserting archive org")

      case Right(_) => {
        contentCollection.insertCollection(archiveOrg.id, coll, Permission.Write) match {
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

  lazy val itemDao = new SalatVersioningDao[Item] {

    def db: MongoDB = SalatServices.this.db

    protected def collectionName: String = "content"

    protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

    protected implicit def context: Context = SalatServices.this.context

    override def checkCurrentCollectionIntegrity: Boolean = false
  }

  lazy val copyFiles: (String, String) => Unit = s3.copyObject(aws.bucket, _, aws.bucket, _)
  lazy val deleteFiles: (String) => Unit = s3.deleteObject(aws.bucket, _)

  lazy val itemAssetService: interface.item.ItemAssetService = new ItemAssetService(
    s3.copyObject(aws.bucket, _, aws.bucket, _),
    s3.deleteObject(aws.bucket, _))

  /**
   * Note:
   * There are some circular dependencies, which require call-by-name.
   * Later versions of macwire support this but not the version for 2.10:
   * @see: https://github.com/adamw/macwire/pull/29
   *
   * For now going to manually build the objects
   */
  lazy val contentCollection = new ContentCollectionService(contentCollectionDao, context, org, item, mode)
  lazy val org: interface.OrganizationService = new OrganizationService(orgDao, context, contentCollection, metadataSet, item, mode)

  lazy val token: interface.auth.AccessTokenService = wire[AccessTokenService]

  lazy val assessmentTemplate: interface.assessment.AssessmentTemplateService = wire[AssessmentTemplateService]

  lazy val metadata: interface.metadata.MetadataService = wire[MetadataService]

  lazy val apiClient: interface.auth.ApiClientService = wire[ApiClientService]

  lazy val user: interface.UserService = wire[UserService]

  lazy val registrationToken: interface.RegistrationTokenService = wire[RegistrationTokenService]

  lazy val metadataSet: interface.metadata.MetadataSetService = new MetadataSetService(metadataSetDao, context, org)

  lazy val item: interface.item.ItemService = new ItemService(itemDao, itemAssetService, contentCollection, context, archiveConfig)

  lazy val assessment: interface.assessment.AssessmentService = wire[AssessmentService]

  override def subject: SubjectService = ???

  override def standard: StandardService = ???

  override def fieldValue: FieldValueService = ???
}
