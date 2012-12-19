package common.views.helpers

import com.mongodb.casbah.commons.MongoDBObject
import models.FieldValue

object Defaults{

  lazy val fieldValues : String = FieldValue.findOne(MongoDBObject()) match {
    case Some(fv) => com.codahale.jerkson.Json.generate(fv)
    case _ => ""
  }
}