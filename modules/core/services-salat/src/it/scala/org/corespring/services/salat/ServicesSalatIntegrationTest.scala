package org.corespring.services.salat

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.services.salat.bootstrap._
import org.corespring.services.salat.it.DbSingleton
import org.joda.time.DateTime
import org.specs2.execute.{ AsResult, Result }
import org.specs2.mock.Mockito
import org.specs2.mutable.{ Around, Specification }

trait ServicesSalatIntegrationTest extends Specification with Mockito with Around {

  sequential

  val contentCollectionId = ObjectId.get
  val orgId = ObjectId.get

  private val logger = Logger(classOf[ServicesSalatIntegrationTest])

  lazy val s3 = mock[AmazonS3]

  lazy val services = new SalatServices {
    override def db: MongoDB = DbSingleton.db

    override def archiveConfig: ArchiveConfig = ArchiveConfig(contentCollectionId, orgId)

    override def bucket: Bucket = Bucket(System.getenv("AWS_BUCKET"))

    override def s3: AmazonS3 = ServicesSalatIntegrationTest.this.s3

    override def accessTokenConfig: AccessTokenConfig = AccessTokenConfig()

    override implicit def context: Context = new ServicesContext(this.getClass.getClassLoader)

    override def mostRecentDateModifiedForSessions: (Seq[ObjectId]) => Option[DateTime] = _ => None
  }

  override def around[T](r: => T)(implicit toResult: AsResult[T]): Result = {
    logger.debug(s"function=around - dropping db")
    DbSingleton.db.dropDatabase()
    AsResult(r)
  }

}
