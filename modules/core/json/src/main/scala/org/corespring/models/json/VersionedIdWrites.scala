package org.corespring.models.json

import org.bson
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._

object VersionedIdFormat extends Format[VersionedId[ObjectId]] {

  def reads(json: JsValue): JsResult[VersionedId[bson.types.ObjectId]] = json match {
    case JsString(text) => VersionedId(text).map(JsSuccess(_)).getOrElse(throw new RuntimeException("Can't parse json"))
    case _ => JsError("Should be a string")
  }

  def writes(id: VersionedId[bson.types.ObjectId]): JsValue = {

    def intString(v: Any): String = {
      val rawString = v.toString
      rawString.split("\\.")(0)
    }
    /**
     * Note: We are experiencing some weird runtime boxing/unboxing which means that
     * sometimes the version is passed as Some(Long) instead of Some(Int)
     * To work around this we cast to Any
     * TODO: find out what is causing this? new scala version? new play version?
     * Note: This appears to only happen when you make requests via the play test framework.
     */
    val out = id.id.toString + id.version.map { v: Any => ":" + intString(v) }.getOrElse("")
    JsString(out)
  }
}

