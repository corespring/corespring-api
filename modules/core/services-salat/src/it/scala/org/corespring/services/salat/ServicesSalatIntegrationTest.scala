package org.corespring.services.salat

import com.amazonaws.services.s3.AmazonS3
import org.bson.types.ObjectId
import org.corespring.services.salat.bootstrap._
import org.corespring.it.mongo.DbSingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ServicesSalatIntegrationTest extends Specification with Mockito {

  sequential

  val contentCollectionId = ObjectId.get
  val orgId = ObjectId.get

  lazy val s3 = mock[AmazonS3]

  lazy val services = new SalatServices(
    DbSingleton.db,
    new ServicesContext(this.getClass.getClassLoader),
    AwsBucket(System.getenv("AWS_BUCKET")),
    ArchiveConfig(contentCollectionId, orgId),
    AccessTokenConfig(),
    s3)

}
