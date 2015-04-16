package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemPublishingService, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Validation, Success, Failure }

class ItemDraftsTest extends Specification with Mockito {

  val itemId = VersionedId(ObjectId.get, Some(0))
  val ed = OrgAndUser(SimpleOrg(ObjectId.get, "ed-org"), None)
  val gwen = OrgAndUser(SimpleOrg(ObjectId.get, "gwen-org"), None)
  val item = Item(id = itemId)

  private def mockWriteResult(ok: Boolean = true, err: String = "mock mongo error"): WriteResult = {
    val m = mock[WriteResult]
    m.getLastError.returns {
      val cr = mock[CommandResult]
      cr.ok returns ok
      cr.getErrorMessage returns err
    }
    m
  }

  private def bump(vid: VersionedId[ObjectId]): VersionedId[ObjectId] = vid.copy(version = vid.version.map { n => n + 1 })

  trait MockItemDrafts extends ItemDrafts {

    trait IS extends ItemService with ItemPublishingService
    val mockItemService = {
      val m = mock[IS]
      m.save(any[Item], any[Boolean]).answers {
        (obj, mock) =>
          val arr: Array[Any] = obj.asInstanceOf[Array[Any]]
          val createNewVersion = arr(1).asInstanceOf[Boolean]
          println(s"createNewVersion: $createNewVersion")
          Right(if (createNewVersion) bump(itemId) else itemId)
      }
      m.findOneById(any[VersionedId[ObjectId]]) returns Some(item)
      m.isPublished(any[VersionedId[ObjectId]]) returns false
      m
    }

    val mockDraftService = {
      val m = mock[ItemDraftService]
      m.save(any[ItemDraft]) returns mockWriteResult()
      m.owns(any[OrgAndUser], any[DraftId]) returns true
      m
    }

    val mockAssets = {
      val m = mock[ItemDraftAssets]
      m.copyDraftToItem(any[DraftId], any[VersionedId[ObjectId]]).answers { (obj, mock) =>
        val arr = obj.asInstanceOf[Array[Any]]
        val d = arr(1).asInstanceOf[VersionedId[ObjectId]]
        Success(d)
      }
      m.copyItemToDraft(any[VersionedId[ObjectId]], any[DraftId]).answers { (obj, mock) =>
        val arr = obj.asInstanceOf[Array[Any]]
        val d = arr(1).asInstanceOf[DraftId]
        Success(d)
      }
      m.deleteDraft(any[DraftId]) returns Success()
      m
    }
    val mockCommitService = {
      val m = mock[CommitService]
      m.save(any[ItemCommit]) returns mockWriteResult()
      m
    }

    val itemService = mockItemService

    /** Check that the user may create the draft for the given src id */
    override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = true

    val draftService: ItemDraftService = mockDraftService

    val assets: ItemDraftAssets = mockAssets

    val commitService: CommitService = mockCommitService
  }

  def mkItem(isPublished: Boolean) = Item(id = itemId, published = isPublished)
  def mkDraft(u: OrgAndUser, i: Item = item) = ItemDraft(i, u)
  val gwensDraft = mkDraft(gwen)
  val oid = DraftId(item.id, ed)
  def TestError(name: String = "test error") = GeneralError(name)

  "ItemDrafts" should {

    sequential

    "remove" should {

      class __(removeSuccessful: Boolean, owns: Boolean, assetsSuccessful: Boolean) extends Scope with MockItemDrafts {
        mockDraftService.remove(any[DraftId]) returns removeSuccessful
        mockDraftService.owns(any[OrgAndUser], any[DraftId]) returns owns
        mockAssets.deleteDraft(any[DraftId]) returns {
          if (assetsSuccessful) Success(Unit) else Failure(TestError("deleteDraft"))
        }
      }

      "fail if user doesn't own draft" in new __(true, false, false) {
        remove(gwen)(oid) must_== Failure(UserCantRemove(gwen, oid))
      }

      "fail if remove failed" in new __(false, true, false) {
        remove(ed)(oid) must_== Failure(DeleteDraftFailed(oid))
      }

      "fail if assets.deleteDraft failed" in new __(true, true, false) {
        remove(ed)(oid) must_== Failure(TestError("deleteDraft"))
      }
      "succeed" in new __(true, true, true) {
        remove(ed)(oid) must_== Success(oid)
      }
    }

    "publish" should {

      class __(
        load: Boolean,
        val latestSrc: Option[Item],
        itemPublish: Boolean,
        removeDrafts: Boolean) extends Scope with MockItemDrafts {

        val draft = mkDraft(ed, item)

        mockDraftService.load(any[DraftId]) returns {
          if (load) Some(draft) else None
        }

        mockItemService.findOneById(any[VersionedId[ObjectId]]) returns latestSrc
        mockItemService.publish(any[VersionedId[ObjectId]]) returns itemPublish

        mockDraftService.removeNonConflictingDraftsForOrg(any[ObjectId], any[ObjectId]) returns {
          Seq.empty
        }

        mockAssets.deleteDrafts(any[DraftId]) returns {
          Seq(if (removeDrafts) Success(Unit) else Failure(TestError("delete-drafts")))
        }
      }

      "fail if load draft failed" in new __(false, None, false, false) {
        publish(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
      }

      "fail if loading latest src fails" in new __(true, None, false, false) {
        publish(ed)(oid) must_== Failure(CantFindLatestSrc(oid))
      }

      "fail if loading latest src doesnt match draft" in
        new __(true, Some(item.cloneItem), true, false) {
          publish(ed)(oid) must_== Failure(DraftIsOutOfDate(draft, ItemSrc(latestSrc.get)))
        }

      "fail if itemService.publish failed" in
        new __(true, Some(item), false, false) {
          publish(ed)(oid) must_== Failure(PublishItemError(item.id))
        }

      "fail if removeNonConflictingDrafts failed" in
        new __(true, Some(item), true, false) {
          publish(ed)(oid) must_== Failure(RemoveDraftFailed(List(TestError("delete-drafts"))))
        }

      "succeed" in
        new __(true, Some(item), true, true) {
          publish(ed)(oid) must_== Success(item.id)
        }
    }

    "load" should {

      class __(
        owns: Boolean, load: Boolean) extends Scope with MockItemDrafts {

        val draft = mkDraft(ed, item)
        mockDraftService.owns(any[OrgAndUser], any[DraftId]) returns {
          owns
        }
        mockDraftService.load(any[DraftId]) returns {
          if (load) Some(draft) else None
        }
      }

      "fail if user doesn't own draft" in new __(false, false) {
        load(ed)(oid) must_== Failure(UserCantLoad(ed, oid))
      }

      "fail if service.load fails" in new __(true, false) {
        load(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
      }

      "succeed" in new __(true, true) {
        load(ed)(oid) must_== Success(draft)
      }
    }

    "cloneDraft" should {

      class __(
        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")),
        createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")),
        saveSuccess: Boolean = false) extends Scope with MockItemDrafts {

        override def load(user: OrgAndUser)(id: DraftId) = loadResult
        override def create(id: VersionedId[ObjectId], user: OrgAndUser, expires: Option[DateTime]) = createResult

        val draft = mkDraft(ed, item)
        mockItemService.save(any[Item], any[Boolean]) returns {
          if (saveSuccess) Right(itemId) else Left("Err")
        }
      }

      "fail if load fails" in new __() {
        cloneDraft(ed)(oid) must_== Failure(TestError("load"))
      }

      "fail if itemService.save" in new __(Success(mkDraft(ed, item))) {
        cloneDraft(ed)(oid) must_== Failure(SaveDraftFailed("Err"))
      }

      "fail if create fails" in new __(Success(mkDraft(ed, item)), saveSuccess = true) {
        cloneDraft(ed)(oid) must_== Failure(TestError("create"))
      }

      "succeed" in new __(Success(mkDraft(ed, item)), Success(mkDraft(ed, item)), true) {
        cloneDraft(ed)(oid) must_== Success(DraftCloneResult(itemId, oid))
      }
    }

    "create" should {
      class __(
        canCreate: Boolean,
        val getUnpublishedVersion: Option[Item] = None,
        copyResult: Validation[DraftError, DraftId] = Failure(TestError("copyAssets")),
        saveResult: Validation[DraftError, DraftId] = Failure(TestError("save"))) extends Scope with MockItemDrafts {
        override def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = canCreate
        mockItemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion
        mockAssets.copyItemToDraft(any[VersionedId[ObjectId]], any[DraftId]) returns copyResult
        override def save(u: OrgAndUser)(d: ItemDraft) = saveResult
      }

      "fail if userCanCreateDraft fails" in new __(false) {
        create(itemId, ed, None) must_== Failure(UserCantCreate(ed, itemId))
      }

      "fail if itemService.getOrCreateUnpublishedVersion fails" in new __(true) {
        create(itemId, ed, None) must_== Failure(GetUnpublishedItemError(itemId))
      }

      "fail if assets.copyItemToDraft fails" in new __(true, Some(item)) {
        create(itemId, ed, None) must_== Failure(TestError("copyAssets"))
      }

      "fail if save fails" in new __(true, Some(item), Success(oid)) {
        create(itemId, ed, None) must_== Failure(TestError("save"))
      }

      "succeed" in new __(true, Some(item), Success(oid), Success(oid)) {
        create(itemId, ed, None) match {
          case Success(d) => {
            d.src.data must_== getUnpublishedVersion.get
          }
          case _ => failure("should have been successful")
        }
      }
    }

    "loadOrCreate" should {

      class __(
        load: Option[ItemDraft] = None,
        val getUnpublishedVersion: Option[Item] = None,
        createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")))
        extends Scope
        with MockItemDrafts {
        mockDraftService.load(any[DraftId]) returns load
        mockItemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion
        override def create(id: VersionedId[ObjectId], user: OrgAndUser, expires: Option[DateTime] = None) = createResult
      }

      "fail if load fails and create fails" in new __() {
        loadOrCreate(ed)(oid) must_== Failure(TestError("create"))
      }

      "throw an exception if it can't load unpublished item" in new __(Some(mkDraft(ed, item))) {
        loadOrCreate(ed)(oid) must throwA[RuntimeException]
      }

      "not update the item if is has a conflict" in new __(
        Some(mkDraft(ed, item).copy(hasConflict = true)),
        Some(item.cloneItem)) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.src.data must_== item
          case Failure(e) => failure("should have been successful")
        }
      }

      "update the item if is has a conflict" in new __(
        Some(mkDraft(ed, item).copy(hasConflict = false)),
        Some(item.cloneItem)) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.src.data must_== getUnpublishedVersion.get
          case Failure(e) => failure("should have been successful")
        }
      }
    }

    "save" should {

      class __(owns: Boolean = false, saveDraft: Boolean = false) extends Scope with MockItemDrafts {
        mockDraftService.owns(any[OrgAndUser], any[DraftId]) returns owns
        mockDraftService.save(any[ItemDraft]) returns mockWriteResult(saveDraft)
      }

      "fail if user doesn't own" in new __() {
        save(ed)(mkDraft(gwen, item)) must_== Failure(UserCantSave(ed, gwen))
      }

      "fail if save fails" in new __(true) {
        save(ed)(mkDraft(ed, item)) must_== Failure(SaveDataFailed(mockWriteResult(false).getLastError.getErrorMessage))
      }

      "succeed" in new __(true, true) {
        save(ed)(mkDraft(ed, item)) must_== Success(oid)
      }
    }
  }
}
