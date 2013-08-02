package common.seed

import org.corespring.platform.core.models._
import com.mongodb.casbah.{MongoDB, MongoCollection}
import java.io.File
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api._
import play.api.Play.current
import JsonImporter._
import web.models.QtiTemplate
import basiclti.models.LtiQuiz
import developer.models.RegistrationToken
import se.radley.plugin.salat.SalatPlugin
import org.corespring.platform.core.models._
import scala.Some
import org.corespring.platform.core.models.auth.{ApiClient, AccessToken}
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import org.corespring.platform.core.models.quiz.basic.Quiz
import org.corespring.platform.core.models.item.FieldValue

object SeedDb {

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }


  private lazy val collections: List[MongoCollection] = List(

    salatDb()(play.api.Play.current)("versioned_content"),
    salatDb()(play.api.Play.current)("content"),
    AccessToken.collection,
    ContentCollection.collection,
    FieldValue.collection,
    Subject.collection,
    Standard.collection,
    ApiClient.collection,
    DefaultItemSession.collection,
    User.collection,
    Organization.collection,
    QtiTemplate.collection,
    LtiQuiz.collection,
    RegistrationToken.collection,
    Quiz.collection
  )


  abstract class SeedFormat

  case class JsonOnEachLine(f: File) extends SeedFormat

  case class JsonFilesAreChildren(f: File) extends SeedFormat

  case class JsonListFile(f: File) extends SeedFormat

  def emptyData() {
    collections.foreach(_.drop())
  }

  def emptyStaticData() {
    FieldValue.collection.drop()
    QtiTemplate.collection.drop()
  }

  def seedData(path: String) {
    Logger.info("seedData: " + path)

    val folder: File = Play.getFile(path)
    for (file <- folder.listFiles) {
      val basename = file.getName.replace(".json", "")
      collections.find(basename == _.name) match {
        case Some(c) => {
          Logger.info("Seeding: " + c.name)
          getSeedFormat(file) match {
            case JsonOnEachLine(f) => jsonLinesToDb(path + "/" + f.getName, c)
            case JsonFilesAreChildren(f) => insertFilesInFolder(path + "/" + f.getName, c)
            case JsonListFile(f) => jsonFileListToDb(path + "/" + f.getName + "/list.json", c)
          }
        }
        case _ => Logger.warn("Couldn't find collection for: " + file.getName)
      }
    }
  }

  private def getSeedFormat(f: File): SeedFormat = {
    if (f.isDirectory) {
      getSingleList(f) match {
        case Some(listFile) => JsonListFile(f)
        case _ => JsonFilesAreChildren(f)
      }
    } else {
      JsonOnEachLine(f)
    }
  }

  private def getSingleList(directory: File): Option[File] = {

    val listFile = for (f <- directory.listFiles; if (f.getName == "list.json")) yield f

    if (listFile.length != 1) {
      None
    } else {
      Some(listFile(0))
    }
  }


  def addMockAccessToken(token: String, scope: Option[String]) = {
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val accessToken = AccessToken(new ObjectId("502404dd0364dc35bb393397"), scope, token, creationDate, creationDate.plusHours(24), true)
    AccessToken.insert(accessToken)
  }

}
