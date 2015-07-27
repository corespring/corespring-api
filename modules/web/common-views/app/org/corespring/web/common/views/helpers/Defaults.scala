package org.corespring.web.common.views.helpers

import com.typesafe.config.{ ConfigFactory, Config }
import java.util.Properties
import org.corespring.models.item.{ ItemType, FieldValue }
import play.api.Play
import play.api.Play.current
import play.api.libs.json._

object BuildInfo{
   val propsFile = "/buildInfo.properties"

  private val properties = {
    val url = Play.resource(propsFile)
    url.map { u =>
      val input = u.openStream()
      val props = new Properties()
      props.load(input)
      input.close()
      props
    }.getOrElse(new Properties())
  }

  lazy val commitHashShort: String = properties.getProperty("commit.hash", "?")
  lazy val pushDate: String = properties.getProperty("date", "?")
  lazy val branch: String = properties.getProperty("branch", "?")
}

class Defaults(
  fieldValue: => Option[FieldValue],
  itemTypes: Seq[ItemType]) {


  lazy val fieldValues: String = fieldValue match {
    case Some(fv) => {

      import org.corespring.models.json.item.FieldValueWrites

      val json: JsValue = (FieldValueWrites.writes(fv) match {
        case obj: JsObject => {
          val itemTypeJson = Json.obj("v2ItemTypes" ->
            itemTypes.map { it => Json.obj("key" -> it.key, "value" -> it.value) })

          obj.deepMerge(Json.obj("v2ItemTypes" -> itemTypeJson))
        }
        case value: JsValue => value
      })
      Json.stringify(json)
    }
    case _ => "{}"
  }


  def envName(default: String): String = get("ENV_NAME").getOrElse(default)

  val config: Config = ConfigFactory.load()

  private def get(k: String): Option[String] = try {
    Some(config.getString(k))
  } catch {
    case _: Throwable => None
  }

  lazy val newRelicConf: NewRelicConf = getNewRelicConf()

  def getNewRelicConf() = {
    new NewRelicConf(
      enabled = get("newrelic.enabled").getOrElse("false") == "true",
      licenseKey = get("newrelic.license-key").getOrElse(""),
      applicationID = get("newrelic.application-id").getOrElse(""))
  }

}

