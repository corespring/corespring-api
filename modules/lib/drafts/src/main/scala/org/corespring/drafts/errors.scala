package org.corespring.drafts.errors

import org.corespring.drafts.{ Draft, Src, UserDraft, Commit }
import org.joda.time.DateTime

sealed abstract class DraftError(val msg: String)
sealed abstract class CommitError(override val msg: String) extends DraftError(msg)
sealed abstract class UserCant[U](requester: U, owner: U, action: String) extends DraftError(s"User: $requester, can't commit a draft owned by: $owner")

case class LoadDraftFailed(val draftId: String) extends DraftError(s"Can't load draft with id: $draftId")
case class SaveDataFailed(override val msg: String) extends DraftError(msg)
case object DeleteFailed extends DraftError("Deletion failed")

case class GeneralError(override val msg: String) extends DraftError(msg)

case class RemoveDraftFailed(errs: Seq[DraftError]) extends DraftError(s"Remove draft failed: ${errs.mkString(",")}} ")

case class LoadItemFailed[VID](id: VID) extends DraftError(s"Load item failed: $id")

case class DeleteDraftFailed[ID](id: ID) extends DraftError(s"couldn't delete draft with id: ${id.toString}")

case class CantFindLatestSrc[ID](id: ID) extends DraftError(s"can't find latest src for id: $id")

case class CantParseDraftId(s: String) extends DraftError(s"can't parse draft id: $s. It should have the form: 'itemId~name'.")

case class CantParseVersionedId(s: String) extends DraftError(s"can't parse versioned id: $s. It should have the form: 'id:version'.")

case class ItemHasBeenModified[ID](id: ID, srcDateModified: DateTime, draftDateModified: DateTime)
  extends DraftError(s"The item with id: $id has been modified after this draft was created: item.dateModified: $srcDateModified, draft item.dateModified: $draftDateModified")

case class CommitsWithSameSrc[IDV, USER](commits: Seq[Commit[IDV, USER]])
  extends DraftError(s"There are existing data items that come from the same src: ${commits.mkString(",")}.")

case object SaveCommitFailed extends CommitError("Save commit failed")
case class SaveDraftFailed(id: String) extends CommitError(s"Save draft: $id failed")

case class CommitsAfterDraft[VID, USER](commits: Seq[Commit[VID, USER]])
  extends CommitError(s"There have been commits since this draft was created/updated: ${commits.mkString(",")}")

case class PublishItemError[VID](id: VID) extends DraftError(s"Error publishing item: $id")
case class SaveNewUnpublishedItemError[VID](id: VID) extends DraftError(s"Error saving a new version of the item with publish = false: $id")

case class GetUnpublishedItemError[VID](id: VID) extends DraftError(s"Error getting an upublished item with id: $id")

case class CreateDraftFailed(id: String) extends DraftError(s"Create draft: $id failed")

case class CopyAssetsFailed(from: String, to: String) extends DraftError(s"An error occurred copying assets: $from -> $to")
case class DeleteAssetsFailed(path: String) extends DraftError(s"An error occurred deleting assets: $path")

case class UserCantCommit[U](requester: U, owner: U) extends UserCant[U](requester, owner, "commit")
case class UserCantSave[U](requester: U, owner: U) extends UserCant[U](requester, owner, "save")
case class UserCantCreate[U, VID](requester: U, id: VID) extends DraftError(s"User $requester can't create from id $id")
case class UserCantLoad[U, ID](requester: U, id: ID) extends DraftError(s"User $requester can't load draft with id $id")
case class UserCantRemove[U, ID](requester: U, id: ID) extends DraftError(s"User $requester can't remove draft with id $id")

case class DraftIsOutOfDate[ID, VID, SRC_DATA](d: Draft[ID, VID, SRC_DATA], src: Src[VID, SRC_DATA]) extends DraftError("The src has changed since the draft was created.")
