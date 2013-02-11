package common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import models.FieldValue
import web.controllers.utils.ConfigLoader

object Defaults{

  lazy val fieldValues : String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => com.codahale.jerkson.Json.generate(fv)
    case _ => ""
  }

  lazy val commitHash : String = ConfigLoader.get("ENV_CORESPRING_API_COMMIT_HASH").getOrElse("?")
  lazy val pushDate : String = ConfigLoader.get("ENV_CORESPRING_API_PUSH_DATE").getOrElse("?")
}