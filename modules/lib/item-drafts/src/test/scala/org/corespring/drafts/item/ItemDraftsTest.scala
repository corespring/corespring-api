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

import scalaz.{ Success, Failure }

class ItemDraftsTest extends Specification with Mockito {

  val itemId = VersionedId(ObjectId.get, Some(0))
  val ed = OrgAndUser(SimpleOrg(ObjectId.get, "ed-org"), None)
  val gwen = OrgAndUser(SimpleOrg(ObjectId.get, "gwen-org"), None)
  val item = Item(id = itemId)

  private def mockWriteResult(ok: Boolean = true): WriteResult = {
    val m = mock[WriteResult]
    m.getLastError.returns {
      mock[CommandResult].ok returns ok
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
  def TestError = GeneralError("test error")

  "ItemDrafts" should {

    sequential

    "remove" should {

      class __(removeSuccessful: Boolean, owns: Boolean, assetsSuccessful: Boolean) extends Scope with MockItemDrafts {
        mockDraftService.remove(any[DraftId]) returns removeSuccessful
        mockDraftService.owns(any[OrgAndUser], any[DraftId]) returns owns
        mockAssets.deleteDraft(any[DraftId]) returns {
          if (assetsSuccessful) Success(Unit) else Failure(TestError)
        }
      }

      "fail if user doesn't own draft" in new __(true, false, false) {
        remove(gwen)(oid) must_== Failure(UserCantRemove(gwen, oid))
      }

      "fail if remove failed" in new __(false, true, false) {
        remove(ed)(oid) must_== Failure(DeleteDraftFailed(oid))
      }

      "fail if assets.deleteDraft failed" in new __(true, true, false) {
        remove(ed)(oid) must_== Failure(TestError)
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
          Seq(if (removeDrafts) Success(Unit) else Failure(TestError))
        }
      }

      "fail if load draft failed" in new __(false, None, false, false) {
        publish(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
      }

      "fail if loading latest src fails" in new __(true, None, false, false) {
        publish(ed)(oid) must_== Failure(CantFindLatestSrc(oid))
      }

      "fail if loading latest src doesnt match draft" in new __(
        true,
        Some(item.cloneItem),
        true,
        false) {
        publish(ed)(oid) must_== Failure(DraftIsOutOfDate(draft, ItemSrc(latestSrc.get)))
      }

      "fail if itemService.publish failed" in new __(
        true,
        Some(item),
        false,
        false) {
        publish(ed)(oid) must_== Failure(PublishItemError(item.id))
      }

      "fail if removeNonConflictingDrafts failed" in new __(
        true,
        Some(item),
        true,
        false) {
        publish(ed)(oid) must_== Failure(RemoveDraftFailed(List(TestError)))
      }

      "succeed" in new __(
        true,
        Some(item),
        true,
        true) {
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

    /*

    "cloneDraft" should {
      "fail if load fails" in {}.pending
      "fail if itemService.save fails" in {}.pending
      "fail if create fails" in {}.pending
      "succeed" in {}.pending
    }

    "create" should {
      "fail if userCanCreateDraft fails" in {}.pending
      "fail if itemService.getOrCreateUnpublishedVersion fails" in {}.pending
      "fail if mkDraft fails" in {}.pending
      "fail if save fails" in {}.pending
      "succeed" in {}.pending
    }

    "loadOrCreate" should {
      "create if not found" in{}.pending
      "update if not conflicting" in{}.pending
    }

    "save" should{
      "fail if user doesn't own" in{}.pending
      "fail if save fails" in{}.pending
      "fail if saveCommit fails" in{}.pending
      "fail if assets.copyDraftToItem fails" in{}.pending
      "succeed" in {}.pending
    }
    */

  }

}
