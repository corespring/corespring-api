package org.corespring.drafts.errors

import org.corespring.drafts.Commit

sealed abstract class DraftError(msg: String)
sealed abstract class CommitError(msg: String) extends DraftError(msg)

case class SaveDataFailed(msg: String) extends DraftError(msg)
case object DeleteFailed extends DraftError("Deletion failed")

case class DeleteDraftFailed[ID](id: ID) extends DraftError(s"couldn't delete draft with id: ${id.toString}")

case class CommitsWithSameSrc[ID, VERSION, USER](commits: Seq[Commit[ID, VERSION, USER]])
  extends DraftError("There are existing data items that come from the same src")

case object SaveCommitFailed extends CommitError("Save commit failed")
