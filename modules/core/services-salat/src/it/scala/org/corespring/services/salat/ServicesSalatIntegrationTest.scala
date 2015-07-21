package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.services.salat.bootstrap.{ArchiveConfig, AwsConfig, SalatServices}
import org.corespring.it.mongo.DbSingleton
import org.specs2.mutable.Specification

class ServicesSalatIntegrationTest extends Specification {

  sequential

  val contentCollectionId = ObjectId.get
  val orgId = ObjectId.get

  lazy val services = new SalatServices(
    DbSingleton.db,
    new ServicesContext(this.getClass.getClassLoader),
    AwsConfig(System.getenv("AWS_KEY"), System.getenv("AWS_SECRET"), System.getenv("AWS_BUCKET")),
    ArchiveConfig(contentCollectionId, orgId),
    () => false)

}
