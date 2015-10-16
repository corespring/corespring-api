package org.corespring.services.salat.it

import com.mongodb.casbah.{ MongoURI, MongoDB, MongoConnection }

object DbSingleton {
  val dbName = "services-salat-integration-test"
  lazy val envUri = sys.env("CORESPRING_SERVICES_SALAT_TEST_DB_URI")

  lazy val mongoUri = {
    val out = if (envUri == null) "mongodb://localhost:27017/" + dbName else envUri
    println("[DbSingleton] >>> using uri: " + out)
    out
  }

  lazy val uri = MongoURI(mongoUri)
  lazy val connection: MongoConnection = MongoConnection(uri)
  lazy val db: MongoDB = connection.getDB(uri.database.get)
}