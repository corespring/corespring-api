package common.seed

import com.ee.seeder.Seeder
import com.ee.seeder.log.ConsoleLogger.Level
import com.mongodb.casbah.{MongoDB, MongoCollection}
import org.bson.types.ObjectId
import org.corespring.platform.core.models._
import org.corespring.platform.core.models.auth.{ApiClient, AccessToken}
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import org.joda.time.DateTime
import play.api._
import se.radley.plugin.salat.SalatPlugin

object SeedDb {

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  implicit val current = play.api.Play.current

  private lazy val collections: List[MongoCollection] = List(

    salatDb()(current)("versioned_content"),
    salatDb()(current)("content"),
    AccessToken.collection,
    ContentCollection.collection,
    FieldValue.collection,
    Subject.collection,
    Standard.collection,
    ApiClient.collection,
    DefaultItemSession.collection,
    User.collection,
    Organization.collection,
    salatDb()(current)("templates"),
    salatDb()(current)("lti_quizzes"),
    salatDb()(current)("regtokens"),
    salatDb()(current)("quizzes")
  )

  def emptyData() {
    collections.foreach(_.drop())
  }

  def emptyStaticData() {
    FieldValue.collection.drop()
    salatDb()(current)("templates").drop()
  }

  def seedData(path: String) {
    val db : MongoDB = salatDb()
    val seeder = new Seeder(salatDb(), Level.DEBUG)
    seeder.seed(List(path))
  }

  def addMockAccessToken(token: String, scope: Option[String]) = {
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val accessToken = AccessToken(new ObjectId("502404dd0364dc35bb393397"), scope, token, creationDate, creationDate.plusHours(24), true)
    AccessToken.insert(accessToken)
  }

}
