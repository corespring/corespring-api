package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.{ Commit }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

case class ItemCommit(
  draftId: DraftId,
  srcId: VersionedId[ObjectId],
  date: DateTime = DateTime.now)
  extends Commit[VersionedId[ObjectId], OrgAndUser] {
  override def user: OrgAndUser = draftId.user
}

