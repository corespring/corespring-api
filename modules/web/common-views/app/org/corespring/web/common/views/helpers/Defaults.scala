package org.corespring.web.common.views.helpers

import java.util.Properties

import com.typesafe.config.{ Config, ConfigFactory }
import org.corespring.models.item.{ ComponentType, FieldValue }
import play.api.Play
import play.api.Play.current
import play.api.libs.json._

case class BuildInfo(commitHashShort:String, pushDate:String, branch:String){
  lazy val json = Json.obj(
    "commitHash" -> commitHashShort,
    "branch" -> branch,
    "date" -> pushDate)
}

object BuildInfo {

  def apply(app:play.api.Application) : BuildInfo = {
    val propsFile = "/buildInfo.properties"

    val properties = {
      val url = app.resource(propsFile)
      url.map { u =>
        val input = u.openStream()
        val props = new Properties()
        props.load(input)
        input.close()
        props
      }.getOrElse(new Properties())
    }

    BuildInfo(
      commitHashShort  = properties.getProperty("commit.hash", "?"),
      pushDate  = properties.getProperty("date", "?"),
      branch = properties.getProperty("branch", "?")
    )
  }
}

class Defaults(
  fieldValue: => Option[FieldValue],
  itemTypes: Seq[ComponentType]) {

  lazy val fieldValues: String = fieldValue match {
    case Some(fv) => {

      import org.corespring.models.json.item.FieldValueWrites

      val json: JsObject = FieldValueWrites.writes(fv)
      val itemTypeJson = Json.obj("v2ItemTypes" ->
        itemTypes.map { it => Json.obj("key" -> it.componentType, "value" -> it.label) })
      val out = json.deepMerge(Json.obj("v2ItemTypes" -> itemTypeJson))
      Json.stringify(out)
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

