package web.controllers.services

import com.mongodb.casbah.{MongoConnection, MongoURI, MongoCollection}

object DBConnect {

  def getCollection(uriString: String, collection: String): MongoCollection = {
    val uri = MongoURI(uriString)
    val mongo = MongoConnection(uri)
    val db = mongo(uri.database.get)
    uri.username match {
      case Some(foundUsername) => {
        db.authenticate(uri.username.get, uri.password.get.foldLeft("")(_ + _.toString))
      }
      case _ => //don't authenticate
    }
    val dbCollection: MongoCollection = db(collection)
    dbCollection
  }
}


