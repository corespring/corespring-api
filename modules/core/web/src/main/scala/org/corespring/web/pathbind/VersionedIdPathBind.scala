package org.corespring.web.pathbind

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.PathBindable

object VersionedIdPathBind {

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
