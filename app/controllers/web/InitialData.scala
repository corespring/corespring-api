package controllers.web

import _root_.models.web.QtiTemplate
import com.mongodb.casbah.MongoCollection
import io.Source
import java.io.{File, InputStream, FileInputStream}
import java.util.LinkedHashMap
import java.util.ArrayList
import org.yaml.snakeyaml.Yaml
import play.api._
import play.api.Play.current
import com.mongodb.DBObject
import services.{DBConnect, UserService}
import utils.ConfigLoader

object UserData {

  val USER_JSON: String = "conf/users/users.json"

  def insert() = {
    if (UserService.count() == 0) {
      val json = Source.fromFile(USER_JSON).mkString
      UserService.insertFromJson(json)
    }
  }
}

object InitialData {

  val TEMPLATES_YAML: String = "conf/templates/templates.yaml"

  /*
   * Read in some templates from a yaml file and persist them to the db
   */
  def insert(): Unit = {
    insertTemplates()
    insertFieldValues()
  }

  def insertTemplates() {
    val list: List[QtiTemplate] = QtiTemplate.all()

    if (list.length == 0) {
      Logger.logger.info("Global::insert::Seeding db templates")

      val yamlFile = new File(TEMPLATES_YAML)

      if (!yamlFile.exists()) {
        Logger.logger.warn("No yaml file found: ")
        return
      }

      val input: InputStream = new FileInputStream(yamlFile)
      val yaml: Yaml = new Yaml()
      val templateData: Object = yaml.load(input)

      templateData match {

        case list: ArrayList[LinkedHashMap[String, String]] => {
          val iterator = list.iterator()
          while (iterator.hasNext) {
            val item: LinkedHashMap[String, String] = iterator.next()
            val template: QtiTemplate = createFromYamlDeclaration(item)
            QtiTemplate.create(template)
          }
        }
        case _ => Logger.logger.info("unknown yaml definition type")
      }
    }
  }

  def insertFieldValues() {
    //Field values should already be inserted.
    /*val collection : MongoCollection = DBConnect.getCollection(ConfigLoader.get("MONGO_URI").get, "fieldValues")
    if ( collection.count == 0 ){
      val jsonString = io.Source.fromFile( Play.getFile("conf/field-values/field-values.json")).mkString
      collection.drop()
      collection.insert( com.mongodb.util.JSON.parse(jsonString).asInstanceOf[DBObject])
      Logger.info("Initialised Field Values")
    }*/
  }

  def createFromYamlDeclaration(item: LinkedHashMap[String, String]): QtiTemplate = {
    val label = item.get("label")
    val code = item.get("code")
    val xmlFile = item.get("xmlFile")
    val xmlData = scala.io.Source.fromFile(xmlFile).mkString
    QtiTemplate(null, label, code, xmlData)
  }
}