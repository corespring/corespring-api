package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.{ Commit, IdAndVersion }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

case class ItemCommit(srcVid: VersionedId[ObjectId],
  commitVid: VersionedId[ObjectId],
  user: OrgAndUser,
  date: DateTime = DateTime.now) extends Commit[ObjectId, Long, OrgAndUser] {

  import org.corespring.drafts.item.Helpers._

  /** The id and version used as the src for the draft */
  override def srcId: IdAndVersion[ObjectId, Long] = srcVid

  /** The id and version that the commit went to. */
  override def committedId: IdAndVersion[ObjectId, Long] = commitVid
}
