package models.versioning

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.mvc.PathBindable

object VersionedIdImplicits {

  implicit object Reads extends Reads[VersionedId[ObjectId]] {

    def reads(json: JsValue): JsResult[VersionedId[ObjectId]] = json match {
        case JsString(text) => Binders.stringToVersionedId(text).map(JsSuccess(_)).getOrElse(throw new RuntimeException("Can't parse json"))
        case _ => JsError("Should be a string" )
      }

  }

  implicit object Writes extends Writes[VersionedId[ObjectId]] {
    def writes(id: VersionedId[ObjectId]): JsValue = {
      val out = id.id.toString + id.version.map(":"+ _).getOrElse("")
      JsString(out)
    }
  }


  object Binders {


    def stringToVersionedId(s: String): Option[VersionedId[ObjectId]] = {
      if (s.contains(":")) {
        val arr = s.split(":")
        val id = arr(0)
        val v = arr(1)
        vId(id, int(v))
      } else {
        vId(s)
      }
    }

    def versionedIdToString(id:VersionedId[ObjectId]) : String =  id.version.map( (l : Int) => s"${id.id.toString}:$l").getOrElse(id.id.toString)


    private def vId(id: String, v: Option[Int] = None): Option[VersionedId[ObjectId]] = if (ObjectId.isValid(id)) {
      Some(VersionedId(new ObjectId(id), v))
    }
    else {
      None
    }

    private def int(i: String): Option[Int] = try {
      Some(i.toInt)
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
