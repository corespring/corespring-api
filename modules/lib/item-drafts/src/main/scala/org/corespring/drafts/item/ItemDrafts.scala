package org.corespring.drafts.item

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{ CommandResult, WriteResult }
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ItemDraftDbUtils, CommitService, ItemDraftService}
import org.corespring.drafts.{ Drafts, Src }
import org.corespring.models.item.Item
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.{ItemPublishingService, ItemService}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class DraftCloneResult(itemId: VersionedId[ObjectId], draftId: DraftId)

case class ItemDraftIsOutOfDate(d: ItemDraft, src: Src[VersionedId[ObjectId], Item]) extends DraftIsOutOfDate[DraftId, VersionedId[ObjectId], Item](d, src)

trait ItemDrafts
  extends Drafts[DraftId, VersionedId[ObjectId], Item, OrgAndUser, ItemDraft, ItemCommit, ItemDraftIsOutOfDate] {

  protected val logger = Logger(classOf[ItemDrafts].getName)

  implicit def context: com.novus.salat.Context

  def itemService: ItemService with ItemPublishingService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  def collection = draftService.collection

  private val utils = new ItemDraftDbUtils{
    override implicit def context: Context = ItemDrafts.this.context
  }

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(itemId: ObjectId, user: OrgAndUser): Boolean
  protected def userCanDeleteDrafts(itemId: ObjectId, user: OrgAndUser): Boolean

  def owns(user: OrgAndUser)(id: DraftId) = draftService.owns(user, id)

  def removeByItemId(user: OrgAndUser)(itemId: ObjectId): Validation[DraftError, ObjectId] = {
    logger.debug(s"function=removeByItemId, itemId=$itemId")
    for {
      _ <- if (userCanDeleteDrafts(itemId, user))
        Success(true)
      else
        Failure(UserCantDeleteMultipleDrafts(user, itemId))
      _ <- if (draftService.removeByItemId(itemId))
        Success(true)
      else
        Failure(GeneralError(s"error removing by item id: $itemId"))
      _ <- assets.deleteDraftsByItemId(itemId)
    } yield itemId
  }

  def remove(user: OrgAndUser)(id: DraftId) = {
    logger.debug(s"function=remove, id=$id")
    for {
      _ <- if (owns(user)(id)) Success(true) else Failure(UserCantRemove(user, id))
      _ <- draftService.remove(id) match {
        case true => Success()
        case false => Failure(DeleteDraftFailed(id))
      }
      _ <- assets.deleteDraft(id)
    } yield id
  }

  def listForOrg(orgId: ObjectId) = draftService.listForOrg(orgId)

  def listByItemAndOrgId(itemId: VersionedId[ObjectId], orgId: ObjectId) = draftService.listByItemAndOrgId(itemId, orgId).toSeq

  def load(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, ItemDraft] = {
    if (draftService.owns(user, draftId)) {
      draftService.load(draftId).toSuccess(LoadDraftFailed(draftId.toString))
    } else {
      Failure(UserCantLoad(user, draftId))
    }
  }

  def cloneDraft(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, DraftCloneResult] = for {
    d <- load(user)(draftId)
    cloned <- Success(d.change.data.cloneItem)
    vid <- itemService.save(cloned).disjunction.validation.leftMap { s => SaveDraftFailed(s.message) }
    _ <- assets.copyDraftToItem(draftId, vid)
    newDraft <- create(draftId, user)
  } yield DraftCloneResult(vid, newDraft.id)

  /**
   * Creates a draft for the target data.
   */
  override def create(draftId: DraftId, user: OrgAndUser, expires: Option[DateTime]): Validation[DraftError, ItemDraft] = {

    logger.debug(s"function=create, draftId=$draftId, user=$user, expires=$expires")

    def mkDraft(id: DraftId, src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
      require(src.published == false, s"You can only create an ItemDraft from an unpublished item: ${src.id}")
      val draft = ItemDraft(draftId, src, user)
      logger.trace(s"function=mkDraft, itemId=${src.id}, draftId=${draft.id}, copy item assets to draft")
      assets.copyItemToDraft(src.id, draft.id).map { _ => draft }
    }

    for {
      canCreate <- if (userCanCreateDraft(draftId.itemId, user)) Success(true) else Failure(UserCantCreate(user, draftId.itemId))
      unpublishedItem <- itemService.getOrCreateUnpublishedVersion(
        new VersionedId[ObjectId](draftId.itemId, None)).toSuccess(GetUnpublishedItemError(draftId.itemId))
      draft <- mkDraft(draftId, unpublishedItem, user)
      saved <- save(user)(draft)
    } yield draft
  }

  def conflict(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, Option[Conflict]] = {
    for {
      d <- draftService.load(draftId).toSuccess(LoadDraftFailed(draftId.toIdString))
      i <- itemService.getOrCreateUnpublishedVersion(d.parent.id).toSuccess(CantFindLatestSrc(d.parent.id))
    } yield {
      if (d.parent.data == i) {
        logger.debug(s"function=conflict, the parent matches the item - no conflicts found")
        None
      } else {
        Some(Conflict(d, i))
      }
    }
  }

  /** load a draft for the src <VID> for that user if not conflicted, if not found create it */
  override def loadOrCreate(user: OrgAndUser)(id: DraftId, ignoreConflict: Boolean = false): Validation[DraftError, ItemDraft] = {
    val draft: Option[ItemDraft] = draftService.load(id)

    def failIfConflicted(d: ItemDraft): Validation[DraftError, ItemDraft] = {
      itemService.getOrCreateUnpublishedVersion(d.parent.id).map { i =>
        logger.debug(s"function=failIfConflicted, ignoreConflict=$ignoreConflict, hasSrcChanged(parent, latest)=${hasSrcChanged(d.parent.data, i)}")
        if (!hasSrcChanged(d.parent.data, i) || ignoreConflict) {
          Success(d)
        } else {
          Failure(draftIsOutOfDate(d, ItemSrc(i)))
        }
      }.getOrElse(Failure(GeneralError("can't find unpublished version")))
    }

    draft.map { d =>
      //If the draft hasn't any local changes
      if (!hasSrcChanged(d.parent.data, d.change.data)) {
        create(id, user)
      } else {
        failIfConflicted(d)
      }
    }.getOrElse(create(id, user))
  }

  override def hasSrcChanged(a: Item, b: Item) = {
    val taskInfo = a.taskInfo != b.taskInfo
    val playerDef = a.playerDefinition != b.playerDefinition
    val supportingMaterials = a.supportingMaterials != b.supportingMaterials
    val collectionId = a.collectionId != b.collectionId
    val standards = a.standards != b.standards
    val reviewsPassed = a.reviewsPassed != b.reviewsPassed
    val reviewsPassedOther = a.reviewsPassedOther != b.reviewsPassedOther
    val otherAlignments = a.otherAlignments != b.otherAlignments
    val contributorDetails = a.contributorDetails != b.contributorDetails
    val priorUse = a.priorUse != b.priorUse
    val priorUseOther = a.priorUseOther != b.priorUseOther
    val priorGradeLevels = a.priorGradeLevels != b.priorGradeLevels

    logger.debug(s"function=hasSrcChanged, taskInfo=$taskInfo, playerDef=$playerDef, supportingMaterials=$supportingMaterials, collectionId=$collectionId")
    Seq(taskInfo, playerDef, supportingMaterials, collectionId, standards,
      reviewsPassed, reviewsPassedOther, otherAlignments, contributorDetails,
      priorUse, priorUseOther, priorGradeLevels).reduce(_ || _)
  }

  def discardDraft(user: OrgAndUser)(id: DraftId) = remove(user)(id)

  private def noVersion(i: Item) = i.copy(id = i.id.copy(version = None))

  protected def saveCommit(c: ItemCommit): Validation[DraftError, Unit] = {
    commitService.save(c).failed(SaveCommitFailed)
  }

  override def save(user: OrgAndUser)(d: ItemDraft): Validation[DraftError, DraftId] = {
    if (draftService.owns(user, d.id)) {
      draftService.save(d)
        .failed(e => SaveDataFailed(e.getErrorMessage))
        .map(_ => d.id)
    } else {
      Failure(UserCantSave(user, d.user))
    }
  }

  override protected def copySrcToDraft(src: Item, draft: ItemDraft): Validation[DraftError, ItemDraft] = {
    val update = draft.copy(parent = ItemSrc(src), change = ItemSrc(src))
    for {
      _ <- draftService.save(update).failed(e => SaveDataFailed(e.getErrorMessage))
      _ <- assets.copyItemToDraft(src.id, draft.id)
    } yield update
  }

  override protected def copyDraftToSrc(d: ItemDraft): Validation[DraftError, ItemCommit] = {
    for {
      vid <- itemService.save(noVersion(d.change.data), false).disjunction.validation.leftMap { s => SaveDataFailed(s.message) }
      commit <- Success(ItemCommit(d.id, d.user, d.change.data.id))
      _ <- saveCommit(commit)
      _ <- assets.copyDraftToItem(d.id, commit.srcId)
      // we also need to reset the draft so that subsequent checks will load
      _ <- Success(logger.debug(s"reset the draft parent"))
      _ <- draftService.save(d.copy(parent = d.change)).failed(SaveDraftFailed(d.id.toIdString))
    } yield commit
  }

  /**
   * Check that the draft src matches the latest src,
   * so that a commit is possible.
   */
  override def getLatestSrc(d: ItemDraft): Option[ItemSrc] = itemService.findOneById(d.parent.id.copy(version = None)).map(i => ItemSrc(i))

  private implicit class WriteResultToValidation(w: WriteResult) {
    require(w != null)

    def failed(e: DraftError): Validation[DraftError, Unit] = failed(_ => e)

    def failed(e: CommandResult => DraftError): Validation[DraftError, Unit] = if (w.getLastError.ok) {
      Success()
    } else {
      Failure(e(w.getLastError))
    }
  }

  override def draftIsOutOfDate(d: ItemDraft, src: Src[VersionedId[ObjectId], Item]): ItemDraftIsOutOfDate = {
    ItemDraftIsOutOfDate(d, src)
  }


  def addFileToChangeSet(draft: ItemDraft, f: StoredFile): Boolean = {


    val query = utils.idToDbo(draft.id)
    val dbo = com.novus.salat.grater[StoredFile].asDBObject(f)
    val update = MongoDBObject("$addToSet" -> MongoDBObject("change.data.playerDefinition.files" -> dbo))
    val result = draftService.collection.update(query, update, false, false)
    logger.trace(s"function=addFileToChangeSet, draftId=${draft.id}, docsChanged=${result.getN}")
    require(result.getN == 1, s"Exactly 1 document with id: ${draft.id} must have been updated")
    result.getN == 1
  }
}
