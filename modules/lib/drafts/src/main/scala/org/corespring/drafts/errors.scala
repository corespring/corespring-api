package org.corespring.drafts.errors

import org.corespring.drafts.Commit

sealed abstract class DraftError(val msg: String)
sealed abstract class CommitError(override val msg: String) extends DraftError(msg)

case class SaveDataFailed(override val msg: String) extends DraftError(msg)
case object DeleteFailed extends DraftError("Deletion failed")

case class DeleteDraftFailed[ID](id: ID) extends DraftError(s"couldn't delete draft with id: ${id.toString}")

case class CommitsWithSameSrc[ID, VERSION, USER](commits: Seq[Commit[ID, VERSION, USER]])
  extends DraftError("There are existing data items that come from the same src")

case object SaveCommitFailed extends CommitError("Save commit failed")

case class CopyAssetsFailed(from: String, to: String) extends DraftError(s"An error occurred copying assets: $from -> $to")
case class DeleteAssetsFailed(path: String) extends DraftError(s"An error occurred deleting assets: $path")
