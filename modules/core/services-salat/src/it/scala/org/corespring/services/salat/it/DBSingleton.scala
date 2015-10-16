package org.corespring.services.salat.it

import com.mongodb.casbah.{ MongoURI, MongoDB, MongoConnection }

object DbSingleton {
  lazy val mongoUri = sys.env
    .get("CORESPRING_SERVICES_SALAT_TEST_DB_URI")
    .getOrElse(s"mongodb://localhost:27017/services-salat-integration-test")

  lazy val uri = MongoURI(mongoUri)
  lazy val dbName = uri.database.get
  lazy val connection: MongoConnection = MongoConnection(uri)
  lazy val db: MongoDB = connection.getDB(uri.database.get)
}