package org.corespring.web.common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.config.{ ConfigFactory, Config }
import org.corespring.platform.core.models.item.FieldValue
import play.api.libs.json.{ JsValue, Json }

object Defaults {

  lazy val fieldValues: String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => {
      import org.corespring.platform.core.models.json._
      implicit val writes = Json.writes[FieldValue]

      val json: JsValue = writes.writes(fv)
      Json.stringify(json)
    }
    case _ => ""
  }

  lazy val commitHashShort: String = get("ENV_CORESPRING_API_COMMIT_HASH_SHORT").getOrElse("?")
  lazy val commitHash: String = get("ENV_CORESPRING_API_COMMIT_HASH").getOrElse("?")
  lazy val commitMsg: String = get("ENV_CORESPRING_API_COMMIT_MSG").getOrElse("?")
  lazy val pushDate: String = get("ENV_CORESPRING_API_PUSH_DATE").getOrElse("?")
  lazy val branch: String = get("ENV_CORESPRING_API_BRANCH").getOrElse("?")

  def envName(default: String): String = get("ENV_NAME").getOrElse(default)

  val config: Config = ConfigFactory.load()

  private def get(k: String): Option[String] = try {
    Some(config.getString(k))
  } catch {
    case _: Throwable => None
  }

}