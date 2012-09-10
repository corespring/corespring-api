package web.config

import java.io.{File, InputStream, FileInputStream}
import java.util.LinkedHashMap
import java.util.ArrayList
import org.yaml.snakeyaml.Yaml
import play.api._
import web.models.QtiTemplate


object InitialData {

  val TEMPLATES_YAML: String = "conf/templates/templates.yaml"

  /*
   * Read in some templates from a yaml file and persist them to the db
   */
  def insert() {
    insertTemplates()
  }

  def insertTemplates() {
    val list: List[QtiTemplate] = QtiTemplate.findAll().toList

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
            QtiTemplate.insert(template)
          }
        }
        case _ => Logger.logger.info("unknown yaml definition type")
      }
    }
  }

  def createFromYamlDeclaration(item: LinkedHashMap[String, String]): QtiTemplate = {
    val label = item.get("label")
    val code = item.get("code")
    val xmlFile = item.get("xmlFile")
    val xmlData = scala.io.Source.fromFile(xmlFile).mkString
    QtiTemplate(null, label, code, xmlData)
  }
}