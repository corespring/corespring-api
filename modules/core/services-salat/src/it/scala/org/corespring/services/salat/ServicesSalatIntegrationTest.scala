package org.corespring.services.salat

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.appConfig.{ Bucket, ArchiveConfig, AccessTokenConfig }
import org.corespring.services.salat.bootstrap._
import org.corespring.services.salat.it.DbSingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ServicesSalatIntegrationTest extends Specification with Mockito {

  sequential

  val contentCollectionId = ObjectId.get
  val orgId = ObjectId.get

  lazy val s3 = mock[AmazonS3]

  lazy val services = new SalatServices {
    override def db: MongoDB = DbSingleton.db

    override def archiveConfig: ArchiveConfig = ArchiveConfig(contentCollectionId, orgId)

    override def bucket: Bucket = Bucket(System.getenv("AWS_BUCKET"))

    override def s3: AmazonS3 = ServicesSalatIntegrationTest.this.s3

    override def accessTokenConfig: AccessTokenConfig = AccessTokenConfig()

    override implicit def context: Context = new ServicesContext(this.getClass.getClassLoader)
  }

}
