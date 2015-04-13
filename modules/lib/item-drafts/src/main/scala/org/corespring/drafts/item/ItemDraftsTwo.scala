package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.{ Drafts, Src }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemPublishingService, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.Logger

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemDraftsTwo
  extends Drafts[ObjectId, VersionedId[ObjectId], Item, OrgAndUser, ItemDraft, ItemCommit] {

  protected val logger = Logger(classOf[ItemDrafts].getName)

  def itemService: ItemService with ItemPublishingService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean

  /**
   * Creates a draft for the target data.
   */
  override def create(id: VersionedId[ObjectId], user: OrgAndUser, expires: Option[DateTime]): Validation[DraftError, ItemDraft] = {
    //if item is published create new unpublished item
    //create draft
    //save draft
    //copy assets
    //return draft
    def mkDraft(srcId: VersionedId[ObjectId], src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
      require(src.published == false, s"You can only create an ItemDraft from an unpublished item: ${src.id}")
      val draft = ItemDraft(src, user)
      assets.copyItemToDraft(src.id, draft.id).map { _ => draft }
    }

    for {
      canCreate <- if (userCanCreateDraft(id, user)) Success(true) else Failure(UserCantCreate(user, id))
      unpublishedItem <- itemService.getOrCreateUnpublishedVersion(id).toSuccess(GetUnpublishedItemError(id))
      draft <- mkDraft(id, unpublishedItem, user)
      saved <- save(user)(draft)
    } yield draft
  }

  /** load a draft for the src <VID> for that user */
  override def load(requester: OrgAndUser)(srcId: VersionedId[ObjectId]): Validation[DraftError, ItemDraft] = {
    draftService
      .load(DraftId(srcId.id, requester))
      .map { d =>

        if (d.hasConflict) {
          Success(d)
        } else {
          itemService.getOrCreateUnpublishedVersion(d.src.id).map { i =>
            val update = d.copy(src = ItemSrc(i), change = ItemSrc(i))
            draftService.save(update)
            update
          }
        }
      }
      .getOrElse(create(srcId, requester))
  }

  /** save a draft */
  override def save(requester: OrgAndUser)(d: ItemDraft): Validation[DraftError, ObjectId] = {
    //save the draft
  }

  override protected def copyDraftToSrc(d: ItemDraft): Validation[DraftError, ItemCommit] = {

  }

  /**
   * Check that the draft src matches the latest src,
   * so that a commit is possible.
   */
  override def getLatestSrc(d: ItemDraft): Src[VersionedId[ObjectId], Item] = ???
}
