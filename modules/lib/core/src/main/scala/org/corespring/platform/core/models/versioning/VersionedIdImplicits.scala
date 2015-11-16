package org.corespring.platform.core.models.versioning

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.mvc.PathBindable

object VersionedIdImplicits {

  implicit object Reads extends Reads[VersionedId[ObjectId]] {

    def reads(json: JsValue): JsResult[VersionedId[ObjectId]] = json match {
      case JsString(text) => Binders.stringToVersionedId(text).map(JsSuccess(_)).getOrElse(throw new RuntimeException("Can't parse json"))
      case _ => JsError("Should be a string")
    }

  }

  implicit object Writes extends Writes[VersionedId[ObjectId]] {
    def writes(id: VersionedId[ObjectId]): JsValue = {

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

  object Binders {

    def stringToVersionedId(s: String): Option[VersionedId[ObjectId]] = {
      if (s.contains(":")) {
        val arr = s.split(":")
        val id = arr(0)
        val v = arr(1)
        vId(id, long(v))
      } else {
        vId(s)
      }
    }

    def versionedIdToString(id: VersionedId[ObjectId]): String = id.version.map((l: Any) => s"${id.id.toString}:$l").getOrElse(id.id.toString)

    private def vId(id: String, v: Option[Long] = None): Option[VersionedId[ObjectId]] = if (ObjectId.isValid(id)) {
      Some(VersionedId(new ObjectId(id), v))
    } else {
      None
    }

    private def long(i: String): Option[Long] = try {
      Some(i.toLong)
    } catch {
      case _: Throwable => None
    }

    implicit def versionedIdPathBindable = new PathBindable[VersionedId[ObjectId]] {
      def bind(key: String, value: String) = {
        stringToVersionedId(value)
          .map(Right(_))
          .getOrElse(Left("Invalid object id for key: " + key))
      }

      def unbind(key: String, value: VersionedId[ObjectId]) = versionedIdToString(value)
    }
  }

}