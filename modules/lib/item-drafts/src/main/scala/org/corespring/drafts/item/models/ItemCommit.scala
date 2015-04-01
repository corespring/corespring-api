package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.{ Commit }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

case class ItemCommit(srcId: VersionedId[ObjectId],
  committedId: VersionedId[ObjectId],
  user: OrgAndUser,
  date: DateTime = DateTime.now)
  extends Commit[VersionedId[ObjectId], OrgAndUser]

