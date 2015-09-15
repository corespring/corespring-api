package org.corespring.services.salat.it

import com.mongodb.casbah.{ MongoURI, MongoDB, MongoConnection }

object DbSingleton {
  val dbName = "corespring_platform_core_test"
  lazy val envUri = System.getenv("CORESPRING_PLATFORM_CORE_TEST_DB_URI")

  lazy val mongoUri = {
    val out = if (envUri == null) "mongodb://localhost:27017/" + dbName else envUri
    println("[DbSingleton] >>> using uri: " + out)
    out
  }

  lazy val uri = MongoURI(mongoUri)
  lazy val connection: MongoConnection = MongoConnection(uri)
  lazy val db: MongoDB = connection.getDB(uri.database.get)
}