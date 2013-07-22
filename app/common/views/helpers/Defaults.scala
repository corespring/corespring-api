package common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import web.controllers.utils.ConfigLoader
import models.item.FieldValue
import play.api.libs.json.{JsValue, Json}

object Defaults{

  lazy val fieldValues : String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => {
      import common.models.json.ObjectIdWrites
      implicit val writes = Json.writes[FieldValue]

      val json : JsValue = writes.writes(fv)
      Json.prettyPrint(json)
    }
    case _ => ""
  }

  lazy val commitHashShort : String = ConfigLoader.get("ENV_CORESPRING_API_COMMIT_HASH_SHORT").getOrElse("?")
  lazy val commitHash : String = ConfigLoader.get("ENV_CORESPRING_API_COMMIT_HASH").getOrElse("?")
  lazy val commitMsg : String = ConfigLoader.get("ENV_CORESPRING_API_COMMIT_MSG").getOrElse("?")
  lazy val pushDate : String = ConfigLoader.get("ENV_CORESPRING_API_PUSH_DATE").getOrElse("?")
  lazy val branch : String = ConfigLoader.get("ENV_CORESPRING_API_BRANCH").getOrElse("?")

  def envName(default:String) : String = ConfigLoader.get("ENV_NAME").getOrElse(default)
}