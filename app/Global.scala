import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.controllers.services.OrgService
import _root_.models.auth.{AccessToken, ApiClient}
import _root_.models.{Content, ContentCollection, Organization}
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.util.JSON
import java.nio.charset.Charset
import org.bson.types.ObjectId
import play.api._
import scala.io.Codec
import com.mongodb.casbah.Imports._
import play.api.Play.current
import com.novus.salat._
import com.novus.salat.global._

/**
 */
object Global extends GlobalSettings {


  override def onStart(app: Application) {

    // support JodaTime
    RegisterJodaTimeConversionHelpers()

    if ( Play.isDev(app) || Play.isTest(app) ) {
      insertTestData("/conf/test-data/")
    }
  }
  def insertTestData(basePath:String) = {
    def jsonToDB(jsonPath:String, coll:MongoCollection) = {
      coll.drop()
      val lines:Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.defaultCharset())).getLines()
      for (line <- lines) {
        coll.insert(JSON.parse(line).asInstanceOf[DBObject],coll.writeConcern)
      }
    }
    jsonToDB(basePath+"orgs.json", Organization.collection)
    jsonToDB(basePath+"items.json", Content.collection)
    jsonToDB(basePath+"collections.json", ContentCollection.collection)
    jsonToDB(basePath+"apiClients.json", ApiClient.collection)
    jsonToDB(basePath+"accessTokens.json", AccessToken.collection)
  }
}
