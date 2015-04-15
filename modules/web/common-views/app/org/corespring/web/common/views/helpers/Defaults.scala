package org.corespring.web.common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.config.{ ConfigFactory, Config }
import java.util.Properties
import org.corespring.platform.core.models.item.FieldValue
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{JsObject, JsArray, JsValue, Json}

object Defaults {

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

  lazy val fieldValues: String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => {
      import org.corespring.platform.core.models.json._
      implicit val writes = Json.writes[FieldValue]

      val json: JsValue = (writes.writes(fv) match {
        case obj: JsObject => obj.deepMerge(Json.obj("v2ItemTypes" -> v2ItemTypes))
        case value: JsValue => value
      })
      Json.stringify(json)
    }
    case _ => "{}"
  }

  lazy val commitHashShort: String = properties.getProperty("commit.hash", "?")
  lazy val pushDate: String = properties.getProperty("date", "?")
  lazy val branch: String = properties.getProperty("branch", "?")

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

  // TODO: Pull these from the index?
  private val v2ItemTypes = JsArray(Map(
    "Drag And Drop" -> "corespring-drag-and-drop",
    "Evaluate an Expression" -> "corespring-function-entry",
    "Graph Lines" -> "corespring-line",
    "Multiple Choice" -> "corespring-multiple-choice",
    "Numbered Lines" -> "corespring-numbered-lines",
    "Open Ended Answer" -> "corespring-extended-text-entry",
    "Ordering" -> "corespring-ordering",
    "Plot Points" -> "corespring-point-intercept",
    "Select Evidence in Text" -> "corespring-select-text",
    "Short Answer - Drop Down" -> "corespring-inline-choice",
    "Short Answer - Enter Text" -> "corespring-text-entry",
    "Visual Choice" -> "corespring-focus-task"
  ).map{ case(value, key) => Json.obj("key" -> key, "value" -> value) }.toSeq)

}
