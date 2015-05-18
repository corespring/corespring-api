package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.core.models.item.{TaskInfo, PlayerDefinition, Item}
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
    override protected def userCanCreateDraft(itemId: ObjectId, user: OrgAndUser): Boolean = true
    override protected def userCanDeleteDrafts(itemId: ObjectId, user: OrgAndUser): Boolean = true

    val draftService: ItemDraftService = mockDraftService

    val assets: ItemDraftAssets = mockAssets

    val commitService: CommitService = mockCommitService

  }

  def mkItem(isPublished: Boolean) = Item(id = itemId, published = isPublished)
  def mkItemWithXhtml(xhtml: String) = item.copy(playerDefinition = Some(PlayerDefinition(xhtml)))
  def mkDraft(u: OrgAndUser, parent: Item = item, change: Item = item) = {
    ItemDraft(
      DraftId(parent.id.id, u.user.map(_.userName).getOrElse("test_user"), u.org.id),
      u,
      ItemSrc(parent),
      ItemSrc(change))
  }
  val gwensDraft = mkDraft(gwen)
  val oid = DraftId(item.id.id, ed.user.map(_.userName).getOrElse("test_user"), ed.org.id)
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
        override def create(id: DraftId, user: OrgAndUser, expires: Option[DateTime]) = createResult

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
        override def userCanCreateDraft(id: ObjectId, user: OrgAndUser): Boolean = canCreate
        mockItemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion
        mockAssets.copyItemToDraft(any[VersionedId[ObjectId]], any[DraftId]) returns copyResult
        override def save(u: OrgAndUser)(d: ItemDraft) = saveResult

        def mkDraftId(itemId: VersionedId[ObjectId], user: OrgAndUser) = DraftId(itemId.id, user.user.map(_.userName).getOrElse("test_user"), user.org.id)
      }

      "fail if userCanCreateDraft fails" in new __(false) {
        create(mkDraftId(itemId, ed), ed, None) must_== Failure(UserCantCreate(ed, itemId.id))
      }

      "fail if itemService.getOrCreateUnpublishedVersion fails" in new __(true) {
        create(mkDraftId(itemId, ed), ed, None) must_== Failure(GetUnpublishedItemError(itemId.id))
      }

      "fail if assets.copyItemToDraft fails" in new __(true, Some(item)) {
        create(mkDraftId(itemId, ed), ed, None) must_== Failure(TestError("copyAssets"))
      }

      "fail if save fails" in new __(true, Some(item), Success(oid)) {
        create(mkDraftId(itemId, ed), ed, None) must_== Failure(TestError("save"))
      }

      "succeed" in new __(true, Some(item), Success(oid), Success(oid)) {
        create(mkDraftId(itemId, ed), ed, None) match {
          case Success(d) => {
            d.parent.data must_== getUnpublishedVersion.get
          }
          case _ => failure("should have been successful")
        }
      }
    }

    "loadOrCreate" should {

      class __(
        val load: Option[ItemDraft] = None,
        val getUnpublishedVersion: Option[Item] = None,
        val createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")))
        extends Scope
        with MockItemDrafts {
        mockDraftService.load(any[DraftId]) returns load
        mockItemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion
        override def create(id: DraftId, user: OrgAndUser, expires: Option[DateTime] = None) = createResult
      }

      "fail if load fails and create fails" in new __() {
        loadOrCreate(ed)(oid) must_== Failure(TestError("create"))
      }

      "fail if the draft.parent is out of date and the draft.parent != draft.change" in new __(
        Some(mkDraft(ed, item, item.copy(playerDefinition = Some(PlayerDefinition("Change!"))))),
        Some(item.cloneItem)) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => failure("should have failed")
          case Failure(ItemDraftIsOutOfDate(d, i)) => {
            d must_== load.get
            i must_== ItemSrc(getUnpublishedVersion.get)
          }
          case _ => failure("should have been an out of date error")
        }
      }

      "return a new item if the draft isn't found" in new __(
        None,
        None,
        Success(mkDraft(ed, item))) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft must_== createResult.toOption.get
          case Failure(ItemDraftIsOutOfDate(d, i)) => failure("should have been successful")
          case _ => failure("should have been an out of date error")
        }
      }

      "create a new draft from the item if draft isn't found" in new __(
        None,
        Some(item),
        Success(mkDraft(ed, mkItemWithXhtml("created")))) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.parent.data.playerDefinition.map(_.xhtml) must_== Some("created")
          case Failure(e) => {
            println("error --->")
            println(e)
            failure("should have been successful")
          }
        }
      }

      "create a new draft from the item if the loaded draft doesn't have changes" in new __(
        Some(mkDraft(ed, item)),
        Some(item),
        Success(mkDraft(ed, mkItemWithXhtml("created")))) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.parent.data.playerDefinition.map(_.xhtml) must_== Some("created")
          case Failure(e) => {
            println("error --->")
            println(e)
            failure("should have been successful")
          }
        }
      }

      "return the draft if found and has local changes" in new __(
        Some(mkDraft(ed, item, mkItemWithXhtml("change"))),
        Some(item)) {
        loadOrCreate(ed)(oid) match {
          case Success(draft) => draft must_== load.get
          case Failure(e) => {
            println("error --->")
            println(e)
            failure("should have been successful")
          }
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

    "hasSrcChanged" should {
      class __() extends Scope with MockItemDrafts {
        val item1 = Item(id = itemId)
      }

      "return false if item has not changed" in new __ {
        val item2 = item1.copy()
        hasSrcChanged(item1, item2) must_== false
      }

      "return true if collectionId has changed" in new __ {
        val item2 = item1.copy(collectionId = Some("1234"))
        hasSrcChanged(item1, item2) must_== true
      }

      "return true if taskInfo has changed" in new __ {
        val item2 = item1.copy(taskInfo = Some(TaskInfo()))
        hasSrcChanged(item1, item2) must_== true
      }

      "return true if playerDefinition has changed" in new __ {
        val item2 = item1.copy(playerDefinition = Some(PlayerDefinition("")))
        hasSrcChanged(item1, item2) must_== true
      }

      "return true if supportingMaterials has changed" in new __ {
        val item2 = item1.copy(supportingMaterials = Seq(Resource(name="test", files=Seq.empty)))
        hasSrcChanged(item1, item2) must_== true
      }
    }
  }
}
