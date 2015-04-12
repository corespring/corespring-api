package org.corespring.drafts.item

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.{ Src, Drafts }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.Logger

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

case class DraftCloneResult(itemId: VersionedId[ObjectId], draftId: ObjectId)

/**
 * An implementation of <Drafts> for <Item> backed by some mongo services.
 */
trait ItemDrafts
  extends Drafts[ObjectId, VersionedId[ObjectId], Item, OrgAndUser, ItemDraft, ItemCommit]
  with CommitCheck {

  protected val logger = Logger(classOf[ItemDrafts].getName)

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  /**
   * Check that the draft src matches the latest src,
   * so that a commit is possible.
   */
  override def getLatestSrc(d: ItemDraft): Src[VersionedId[ObjectId], Item] = ItemSrc(itemService.findOneById(d.src.id.copy(version = None)).get)

  private def noVersion(i: Item) = i.copy(id = i.id.copy(version = None))

  override protected def copyDraftToSrc(d: ItemDraft): Validation[DraftError, ItemCommit] = {
    for {
      vid <- itemService.save(noVersion(d.src.data), false).disjunction.validation.leftMap { s => SaveDataFailed(s) }
      commit <- Success(ItemCommit(d.id, d.src.data.id, d.user))
      _ <- saveCommit(commit)
      _ <- assets.copyDraftToItem(d.id, commit.srcId)
    } yield commit
  }

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean

  /**
   * Creates a draft for the target data.
   */
  override def create(id: VersionedId[ObjectId], user: OrgAndUser, expires: Option[DateTime] = None): Validation[DraftError, ItemDraft] = {

    def mkDraft(srcId: VersionedId[ObjectId], src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
      require(src.published == false, s"You can only create an ItemDraft from an unpublished item: ${src.id}")
      val draft = ItemDraft(src, user)
      assets.copyItemToDraft(src.id, draft.id).map { _ => draft }
    }

    for {
      canCreate <- if (userCanCreateDraft(id, user)) Success(true) else Failure(UserCantCreate(user, id))
      newVid <- itemService.saveNewUnpublishedVersion(id).toSuccess(SaveNewUnpublishedItemError(id))
      item <- itemService.findOneById(newVid).toSuccess(LoadItemFailed(id))
      draft <- mkDraft(id, item, user)
      saved <- save(user)(draft)
    } yield draft
  }

  def listForOrg(orgId: ObjectId) = draftService.listForOrg(orgId)

  def list(id: VersionedId[ObjectId]): Seq[ItemDraft] = {
    val version = id.version.getOrElse(itemService.currentVersion(id))
    draftService.findByIdAndVersion(id.id, version)
  }

  /**
   * Publish the draft.
   * Set published to true, commit and delete the draft.
   * @param draftId
   * @return
   * d <- load(requester)(draftId).toSuccess(LoadDraftFailed(draftId.toString))
   * published <- Success(d.mkChange(d.src.data.copy(published = true)))
   * commit <- commit(requester)(published)
   * deleteResult <- removeDraftByIdAndUser(draftId, requester)
   * } yield d.src.data.id
   */
  def publish(user: OrgAndUser)(draftId: ObjectId): Validation[DraftError, VersionedId[ObjectId]] = for {
    d <- load(user)(draftId).toSuccess(LoadDraftFailed(draftId.toString))
    commit <- commit(user)(d)
    publishResult <- if (itemService.publish(commit.srcId)) Success(true) else Failure(PublishItemError(d.src.id))
    deleteResult <- removeDraftByIdAndUser(draftId, user)
  } yield {
    commit.srcId
  }

  def clone(requester: OrgAndUser)(draftId: ObjectId): Validation[DraftError, DraftCloneResult] = for {
    d <- load(requester)(draftId).toSuccess(LoadDraftFailed(draftId.toString))
    itemId <- Success(VersionedId(ObjectId.get))
    vid <- itemService.save(d.src.data.copy(id = itemId, published = false)).disjunction.validation.leftMap { s => SaveDraftFailed(s) }
    newDraft <- create(vid, requester)
  } yield DraftCloneResult(vid, newDraft.id)

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
      draftService.save(d)
        .failed(e => SaveDataFailed(e.getErrorMessage))
        .map(_ => d.id)
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

  protected def saveCommit(c: ItemCommit): Validation[DraftError, Unit] = {
    commitService.save(c).failed(SaveCommitFailed)
  }

  protected def deleteDraft(d: ItemDraft): Validation[DraftError, Unit] = for {
    result <- Success(draftService.remove(d))
    _ <- if (result) Success() else Failure(DeleteDraftFailed(d.id))
    deleteComplete <- assets.deleteDraft(d.id)
  } yield Unit

  private implicit class WriteResultToValidation(w: WriteResult) {
    require(w != null)

    def failed(e: DraftError): Validation[DraftError, Unit] = failed(_ => e)

    def failed(e: CommandResult => DraftError): Validation[DraftError, Unit] = if (w.getLastError.ok) {
      Success()
    } else {
      Failure(e(w.getLastError))
    }
  }

}
