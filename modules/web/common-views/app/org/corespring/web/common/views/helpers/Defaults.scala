package org.corespring.web.common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.config.{ ConfigFactory, Config }
import java.util.Properties
import org.corespring.platform.core.models.item.{ ItemType, FieldValue }
import org.corespring.platform.core.services.item._
import play.api.Play
import play.api.Play.current
import play.api.libs.json._

import scala.concurrent.Await

class Defaults(itemIndexService: ItemIndexService) {

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

  lazy val v2ItemTypes = ItemType.all

}

object Defaults extends Defaults(ElasticSearchItemIndexService)
