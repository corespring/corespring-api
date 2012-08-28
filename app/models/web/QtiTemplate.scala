package models.web


import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger

//import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.MongoURI

case class QtiTemplate(
		_id: ObjectId = new ObjectId,
		label: String,
    code:String,
		xmlData: String
	)

object QtiTemplateDAO extends SalatDAO[QtiTemplate, ObjectId](QtiTemplate.connect())

object QtiTemplate{
	def all(): List[QtiTemplate] = QtiTemplateDAO.find(MongoDBObject.empty).toList

	def create(item: QtiTemplate) : Option[String] = {
		val id : Option[ObjectId] = QtiTemplateDAO.insert(item)
	  id match{
      case Some(uid) => Some(uid.toString)
      case None => None
    }
  }

	def delete(id: String) {
		QtiTemplateDAO.remove(MongoDBObject("_id" -> new ObjectId(id)))
	}

  val MONGO_URI = "MONGO_URI"

  def connect() = {

    val config : Config = ConfigFactory.load()

    val systemConfig = ConfigFactory.systemEnvironment()

    //System env vars take precedence over application.conf vars.

    val uriOptions = List(systemConfig, config)

    val matchingConfig : Option[Config] = uriOptions.find( config => config.hasPath(MONGO_URI) )

    matchingConfig match
    {
      case None => throw new RuntimeException("You must provide a db uri using the variable name: " + MONGO_URI )
      case Some(matchingConfig) =>
      {
        val uriString = matchingConfig.getString(MONGO_URI)
        Logger.logger.info("using mongo uri: " + uriString)
        val uri = MongoURI(uriString)
        val mongo = MongoConnection(uri)
        val db = mongo(uri.database.get)
        db.authenticate(uri.username.get, uri.password.get.foldLeft("")(_ + _.toString))
        db("templates")

      }
    }
	}
}
