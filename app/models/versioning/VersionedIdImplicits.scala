package models.versioning

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.mvc.PathBindable

object VersionedIdImplicits {

  implicit object Reads extends Reads[VersionedId[ObjectId]] {

    def reads(json: JsValue): JsResult[VersionedId[ObjectId]] = json match {
        case JsString(text) => VersionedId(text).map(JsSuccess(_)).getOrElse(throw new RuntimeException("Can't parse json"))
        case _ => JsError("Should be a string" )
      }
  }

  implicit object Writes extends Writes[VersionedId[ObjectId]] {
    def writes(id: VersionedId[ObjectId]): JsValue = {

      /** Note: We are experiencing some weird runtime boxing/unboxing which means that
        * sometimes the version is passed as Some(Long) instead of Some(Int)
        * To work around this we cast to Any
        * TODO: find out what is causing this? new scala version? new play version?
        * Note: This appears to only happen when you make requests via the play test framework.
        */
      val out = id.id.toString + id.version.map{ v : Any => ":"+ v.toString}.getOrElse("")
      JsString(out)
    }
  }


  object Binders {

    @deprecated("VersionedId apply(s:String) now parses the id:version string format", "")
    def stringToVersionedId(s: String): Option[VersionedId[ObjectId]] = VersionedId(s)

    @deprecated("VersionedId toString now generates the id:version string format", "")
    def versionedIdToString(id:VersionedId[ObjectId]) : String = id.toString


    implicit def versionedIdPathBindable = new PathBindable[VersionedId[ObjectId]] {
      def bind(key: String, value: String) = {
        VersionedId(value)
          .map(Right(_))
          .getOrElse(Left("Invalid object id for key: " + key))
      }

      def unbind(key: String, value: VersionedId[ObjectId]) = versionedIdToString(value)
    }
  }

}
