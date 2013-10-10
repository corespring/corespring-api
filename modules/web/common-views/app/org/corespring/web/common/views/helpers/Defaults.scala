package org.corespring.web.common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.config.{ ConfigFactory, Config }
import java.util.Properties
import org.corespring.platform.core.models.item.FieldValue
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{ JsValue, Json }

object Defaults {

  val propsFile = "/buildInfo.properties"

  private lazy val properties = {
    val url = Play.resource("/buildInfo.properties")
    url.map{ u =>
      println(u.getPath)
      val props = new Properties()
      props.load(u.openStream())
      props
    }.getOrElse( new Properties() )
  }

  lazy val fieldValues: String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => {
      import org.corespring.platform.core.models.json._
      implicit val writes = Json.writes[FieldValue]

      val json: JsValue = writes.writes(fv)
      Json.stringify(json)
    }
    case _ => ""
  }

  lazy val commitHashShort: String = properties.getProperty("commit.hash", "?")
  lazy val pushDate: String = properties.getProperty("date", "?" )
  lazy val branch: String = properties.getProperty("branch", "?")

  def envName(default: String): String = get("ENV_NAME").getOrElse(default)

  val config: Config = ConfigFactory.load()

  private def get(k: String): Option[String] = try {
    Some(config.getString(k))
  } catch {
    case _: Throwable => None
  }

}