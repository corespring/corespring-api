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
import org.specs2.time.NoTimeConversions

import scala.concurrent.{ Future, Await, ExecutionContext }
import scala.concurrent.duration._

trait ServicesSalatIntegrationTest extends Specification with Mockito with Around with NoTimeConversions {

  sequential

  protected val archiveContentCollectionId = ObjectId.get
  protected val archiveOrgId = ObjectId.get

  protected def clearDb() = {
    logger.debug(s"function=clearDb - dropping db")

    //Note: for speed we just drop the collection
    val db = DbSingleton.db
    CollectionNames.all.foreach { n =>
      db(n).dropCollection()
    }
  }

  protected val logger = Logger(classOf[ServicesSalatIntegrationTest])

  protected def waitFor[A](f: Future[A]): A = Await.result(f, 1.second)

  lazy val s3 = mock[AmazonS3]

  lazy val services = new SalatServices {
    override def db: MongoDB = DbSingleton.db

    override def archiveConfig: ArchiveConfig = ArchiveConfig(archiveContentCollectionId, archiveOrgId)

    override def bucket: Bucket = Bucket(System.getenv("AWS_BUCKET"))

    override def s3: AmazonS3 = ServicesSalatIntegrationTest.this.s3

    override def accessTokenConfig: AccessTokenConfig = AccessTokenConfig()

    override implicit def context: Context = new ServicesContext(this.getClass.getClassLoader)

    override def mostRecentDateModifiedForSessions: (Seq[ObjectId]) => Option[DateTime] = _ => None

    override def salatServicesExecutionContext: SalatServicesExecutionContext = SalatServicesExecutionContext(ExecutionContext.global)
  }

  override def around[T](r: => T)(implicit toResult: AsResult[T]): Result = {
    logger.debug(s"function=around - dropping db")
    clearDb()
    AsResult(r)
  }

}
