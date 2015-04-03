package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.{ DraftsWithCommitAndCreate }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

/**
 * An implementation of <DraftsWithCommitAndCreate> for <Item> backed by some mongo services.
 */

case class DraftCloneResult(itemId: VersionedId[ObjectId], draftId: ObjectId)

trait ItemDrafts
  extends DraftsWithCommitAndCreate[ObjectId, VersionedId[ObjectId], Item, OrgAndUser, ItemDraft, ItemCommit] {

  protected val logger = Logger("org.corespring.drafts.item.ItemDrafts")

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  def listForOrg(orgId: ObjectId) = draftService.listForOrg(orgId)

  def list(id: VersionedId[ObjectId]): Seq[ItemDraft] = {
    val version = id.version.getOrElse(itemService.currentVersion(id))
    draftService.findByIdAndVersion(id.id, version)
  }

  /**
   * Publish the draft.
   * Set published to true, commit and delete the draft.
   * @param requester
   * @param draftId
   * @return
   */
  def publish(requester: OrgAndUser)(draftId: ObjectId): Validation[DraftError, VersionedId[ObjectId]] = {

    for {
      d <- load(requester)(draftId).toSuccess(LoadDraftFailed(draftId.toString))
      published <- Success(d.update(d.src.data.copy(published = true)))
      commit <- commit(requester)(published)
      deleteResult <- removeDraftByIdAndUser(draftId, requester)
    } yield d.src.data.id
  }

  def clone(requester: OrgAndUser)(draftId: ObjectId): Validation[DraftError, DraftCloneResult] = {
    for {
      d <- load(requester)(draftId).toSuccess(LoadDraftFailed(draftId.toString))
      itemId <- Success(VersionedId(ObjectId.get))
      vid <- itemService.save(d.src.data.copy(id = itemId)).disjunction.validation.leftMap { s => SaveDraftFailed(s) }
      newDraft <- create(vid, requester).toSuccess(CreateDraftFailed(vid.toString))
    } yield DraftCloneResult(vid, newDraft.id)
  }

  //TODO: UT
  override def load(requester: OrgAndUser)(id: ObjectId): Option[ItemDraft] = {
    logger.debug(s"function=load, id=$id")
    draftService.load(id).filter { d =>
      logger.trace(s"function=load, draft.org=${d.user.org}, requester.org=${requester.org}")
      d.user.org == requester.org
    }
  }

  def collection = draftService.collection

  def owns(requester: OrgAndUser)(id: ObjectId): Boolean = draftService.owns(requester, id)

  override def save(requester: OrgAndUser)(d: ItemDraft): Validation[DraftError, ObjectId] = {
    if (d.user == requester) {
      val result = draftService.save(d)
      if (result.getLastError.ok) {
        Success(d.id)
      } else {
        Failure(SaveDataFailed(result.getLastError.getErrorMessage))
      }
    } else {
      Failure(UserCantSave(requester, d.user))
    }
  }

  def removeDraftByIdAndUser(id: ObjectId, user: OrgAndUser): Validation[DraftError, ObjectId] = {
    for {
      result <- Success(draftService.removeDraftByIdAndUser(id, user))
      draftId <- if (result.getN != 1) Failure(DeleteFailed) else Success(id)
      deleteComplete <- assets.deleteDraft(draftId)
    } yield draftId
  }

  override def loadCommitsNotByDraft(draftId: ObjectId, idAndVersion: VersionedId[ObjectId]): Seq[ItemCommit] = {
    require(idAndVersion.version.isDefined, "version must be defined")
    commitService.findByIdAndVersion(idAndVersion.id, idAndVersion.version.get).filterNot(_.draftId == draftId)
  }

  override def commit(requester: OrgAndUser)(d: ItemDraft, force: Boolean = false): Validation[DraftError, ItemCommit] = {
    super.commit(requester)(d, force).flatMap { c =>
      val copyResult = assets.copyDraftToItem(d.id, c.committedId)
      copyResult.map { _ => c }
    }
  }

  override protected def saveCommit(c: ItemCommit): Validation[CommitError, Unit] = {
    val result = commitService.save(c)
    if (result.getLastError.ok) {
      Success()
    } else {
      Failure(SaveCommitFailed)
    }
  }

  /**
   * update the draft src id to the new id
   * @param newSrcId
   * @return
   */
  override protected def updateDraftSrcId(d: ItemDraft, newSrcId: VersionedId[ObjectId]): Validation[DraftError, Unit] = {

    if (d.src.id == newSrcId) {
      Success(Unit)
    } else {
      val update = d.copy(src = d.src.copy(data = d.src.data.copy(id = newSrcId)))

      val result = draftService.save(update)

      if (result.getLastError.ok) {
        Success(Unit)
      } else {
        Failure(SaveDraftFailed(d.id.toString))
      }
    }
  }

  override protected def saveDraftBackToSrc(d: ItemDraft): Validation[DraftError, ItemCommit] = {

    val saveNewVersion = itemService.isPublished(d.src.data.id) || d.src.data.published

    val noVersionId: VersionedId[ObjectId] = d.src.data.id.copy(version = None)
    /**
     * Note: we remove the version because we want to save this as the latest version of the item.
     * If we kept the version and it had been bumped - save would fail.
     */
    val itemWithVersionRemoved = d.src.data.copy(id = noVersionId)
    itemService.save(itemWithVersionRemoved, saveNewVersion) match {
      case Left(err) => Failure(SaveDataFailed(err))
      case Right(vid) => Success(ItemCommit(d.id, d.src.data.id, vid, d.user))
    }
  }

  override protected def findLatestSrc(id: VersionedId[ObjectId]): Option[Item] = itemService.findOneById(id.copy(version = None))

  override protected def mkDraft(srcId: VersionedId[ObjectId], src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
    assets.copyItemToDraft(src.id, ObjectId.get).map { oid =>
      ItemDraft(oid, ItemSrc(src.copy(published = false)), user)
    }
  }

  override protected def deleteDraft(d: ItemDraft): Validation[DraftError, Unit] = for {
    result <- Success(draftService.remove(d))
    _ <- if (result) Success() else Failure(DeleteDraftFailed(d.id))
    deleteComplete <- assets.deleteDraft(d.id)
  } yield Unit

}
