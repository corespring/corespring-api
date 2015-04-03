package org.corespring.drafts.errors

import org.corespring.drafts.Commit

sealed abstract class DraftError(val msg: String)
sealed abstract class CommitError(override val msg: String) extends DraftError(msg)
sealed abstract class UserCant[U](requester: U, owner: U, action: String) extends DraftError(s"User: $requester, can't commit a draft owned by: $owner")

case class LoadDraftFailed(val draftId: String) extends DraftError(s"Can't load draft with id: $draftId")
case class SaveDataFailed(override val msg: String) extends DraftError(msg)
case object DeleteFailed extends DraftError("Deletion failed")

case class DeleteDraftFailed[ID](id: ID) extends DraftError(s"couldn't delete draft with id: ${id.toString}")

case class CommitsWithSameSrc[IDV, USER](commits: Seq[Commit[IDV, USER]])
  extends DraftError("There are existing data items that come from the same src")

case object SaveCommitFailed extends CommitError("Save commit failed")
case class SaveDraftFailed(id: String) extends CommitError(s"Save draft: $id failed")
case class CreateDraftFailed(id: String) extends DraftError(s"Create draft: $id failed")

case class CopyAssetsFailed(from: String, to: String) extends DraftError(s"An error occurred copying assets: $from -> $to")
case class DeleteAssetsFailed(path: String) extends DraftError(s"An error occurred deleting assets: $path")

case class UserCantCommit[U](requester: U, owner: U) extends UserCant[U](requester, owner, "commit")
case class UserCantSave[U](requester: U, owner: U) extends UserCant[U](requester, owner, "save")
