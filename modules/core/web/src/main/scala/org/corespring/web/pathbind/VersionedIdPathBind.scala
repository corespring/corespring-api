package org.corespring.web.pathbind

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.PathBindable

object VersionedIdPathBind {

  def versionedIdToString(id: VersionedId[ObjectId]): String = id.version.map((l: Any) => s"${id.id.toString}:$l").getOrElse(id.id.toString)

  implicit def versionedIdPathBindable = new PathBindable[VersionedId[ObjectId]] {
    def bind(key: String, value: String) = {
      VersionedId(value)
        .map(Right(_))
        .getOrElse(Left("Invalid object id for key: " + key))
    }

    def unbind(key: String, value: VersionedId[ObjectId]) = versionedIdToString(value)
  }
}
