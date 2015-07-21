package org.corespring.services.salat.bootstrap

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{ AmazonS3Client, AmazonS3 }
import com.mongodb.casbah.{ MongoDB }
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring
import org.corespring.models._
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.metadata.MetadataSet
import org.corespring.services.salat.{ UserService, OrganizationService, ContentCollectionService }
import org.corespring.services.salat.item.{ ItemService, ItemAssetService }
import org.corespring.services.salat.metadata.MetadataSetService
import org.corespring.{ services => interface }
import org.corespring.platform.data.mongo.SalatVersioningDao

case class AwsConfig(key: String, secret: String, bucket: String)
case class ArchiveConfig(contentCollectionId: ObjectId, orgId: ObjectId)

class SalatServices(
    db: MongoDB,
    val context: Context,
    aws: AwsConfig,
    archiveConfig: ArchiveConfig,
    @deprecated("This is a legacy function - remove", "0.0.1") isProd: () => Boolean) extends interface.bootstrap.Services {

  private val logger = Logger(classOf[SalatServices])

  RegisterJodaTimeConversionHelpers()

  /**
   * Init the archive org and contentcollection
   */
  private def initArchive(): Unit = {

    val archiveOrg = Organization(
      name = "archive-org",
      id = archiveConfig.orgId,
      isRoot = false)

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

  override lazy val contentCollection: interface.ContentCollectionService = new ContentCollectionService {
    override def itemService: interface.item.ItemService = SalatServices.this.item

    override def organizationService: interface.OrganizationService = SalatServices.this.org

    override def isProd: Boolean = SalatServices.this.isProd()

    override val dao: SalatDAO[ContentCollection, ObjectId] = new SalatDAO[ContentCollection, ObjectId](db("contentcolls")) {}

    override implicit def context: Context = SalatServices.this.context
  }

  override lazy val metadata = new MetadataSetService {
    override def orgService: interface.OrganizationService = SalatServices.this.org

    override val dao: SalatDAO[MetadataSet, ObjectId] = new SalatDAO[MetadataSet, ObjectId](db("metadataSets")) {}

    override implicit def context: Context = SalatServices.this.context
  }

  lazy val itemAsset = new ItemAssetService {

    def bucket: String = aws.bucket

    def s3: AmazonS3 = new AmazonS3Client(new AWSCredentials {
      override def getAWSAccessKeyId: String = aws.key

      override def getAWSSecretKey: String = aws.secret
    })

    override def copyAsset(from: String, to: String): Unit = s3.copyObject(bucket, from, bucket, to)

    override def delete(key: String): Unit = s3.deleteObject(bucket, key)
  }

  override lazy val item: interface.item.ItemService = new ItemService {

    override def archiveCollectionId: ObjectId = SalatServices.this.archiveConfig.contentCollectionId

    override val dao: SalatVersioningDao[Item] = new SalatVersioningDao[Item] {

      def db: MongoDB = SalatServices.this.db

      protected def collectionName: String = "content"

      protected implicit def entityManifest: Manifest[Item] = Manifest.classType(classOf[Item])

      override implicit def context: Context = SalatServices.this.context

      override def checkCurrentCollectionIntegrity: Boolean = false
    }

    override implicit def context: Context = SalatServices.this.context

    override def assets: interface.item.ItemAssetService = SalatServices.this.itemAsset

    override def contentCollectionService: corespring.services.ContentCollectionService = SalatServices.this.contentCollection
  }

  override lazy val org: interface.OrganizationService = new OrganizationService {
    override val dao: SalatDAO[Organization, ObjectId] = new SalatDAO[Organization, ObjectId](db("orgs")) {}

    override def itemService: interface.item.ItemService = SalatServices.this.item

    override implicit def context: Context = SalatServices.this.context

    override def collectionService: interface.ContentCollectionService = SalatServices.this.contentCollection

    override def isProd: Boolean = SalatServices.this.isProd()

    override def metadataSetService: interface.metadata.MetadataSetService = SalatServices.this.metadata
  }

  override lazy val user: interface.UserService = new UserService {
    override val dao: SalatDAO[User, ObjectId] = new SalatDAO[User, ObjectId](db("users")) {}

    override def isProd: Boolean = SalatServices.this.isProd()

    override def orgService: interface.OrganizationService = SalatServices.this.org

    override implicit def context: Context = SalatServices.this.context
  }

  override def registration: interface.RegistrationService = ???
}
