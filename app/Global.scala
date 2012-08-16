import _root_.controllers.auth.Permission
import _root_.controllers.Log
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

    }
  }

}
