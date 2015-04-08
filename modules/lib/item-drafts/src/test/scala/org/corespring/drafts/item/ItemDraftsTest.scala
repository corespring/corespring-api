package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
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

    val mockItemService = {
      val m = mock[ItemService]
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
      m
    }

    val mockAssets = {
      val m = mock[ItemDraftAssets]
      m.copyDraftToItem(any[ObjectId], any[VersionedId[ObjectId]]).answers { (obj, mock) =>
        val arr = obj.asInstanceOf[Array[Any]]
        val d = arr(1).asInstanceOf[VersionedId[ObjectId]]
        Success(d)
      }
      m.copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]).answers { (obj, mock) =>
        val arr = obj.asInstanceOf[Array[Any]]
        val d = arr(1).asInstanceOf[ObjectId]
        Success(d)
      }
      m.deleteDraft(any[ObjectId]) returns Success()
      m
    }
    val mockCommitService = {
      val m = mock[CommitService]
      m.save(any[ItemCommit]) returns mockWriteResult()
      m.findCommitsSince(any[VersionedId[ObjectId]], any[DateTime]) returns Seq.empty
      m
    }

    val itemService: ItemService = mockItemService

    /** Check that the user may create the draft for the given src id */
    override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = true

    val draftService: ItemDraftService = mockDraftService

    val assets: ItemDraftAssets = mockAssets

    val commitService: CommitService = mockCommitService
  }

  def mkItem(isPublished: Boolean) = Item(id = itemId, published = isPublished)
  def mkDraft(u: OrgAndUser, i: Item = item) = ItemDraft(ObjectId.get, ItemSrc(i), u)
  val gwensDraft = mkDraft(gwen)

  "ItemDrafts" should {

    sequential

    "when creating" should {

      class scp(canCreate: Boolean) extends Scope with MockItemDrafts {
        override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = canCreate
      }

      "return error if create" in new scp(false) {
        create(VersionedId(ObjectId.get), ed) must_== None
      }

      "return the item draft" in new scp(true) {
        create(VersionedId(ObjectId.get), ed) match {
          case Some(ItemDraft(_, _, _, _, _, _)) => success
          case _ => failure("should have got an item draft")
        }
      }

      "always uses the latest version of an item as the src for a draft" in new scp(true) {
        create(itemId, ed)
        there was one(itemService).findOneById(itemId.copy(version = None))
      }
    }

    "when saving" should {

      "not allow ed to save gwen's draft" in new Scope with MockItemDrafts {
        save(ed)(gwensDraft) must_== Failure(UserCantSave(ed, gwen))
      }

      "allow gwen to save gwen's draft" in new Scope with MockItemDrafts {
        save(gwen)(gwensDraft) must_== Success(gwensDraft.id)
      }
    }

    "when removing a draft" should {

      class s(n: Int) extends Scope with MockItemDrafts {
        val oid = ObjectId.get
        mockDraftService.removeDraftByIdAndUser(any[ObjectId], any[OrgAndUser]) returns {
          mock[WriteResult].getN.returns(n)
        }
      }

      "return delete failed" in new s(0) {
        removeDraftByIdAndUser(oid, ed) must_== Failure(DeleteFailed)
      }

      "return draft id" in new s(1) {
        removeDraftByIdAndUser(oid, ed) must_== Success(oid)
        there was one(assets).deleteDraft(oid)
      }
    }

    "when committing" should {

      "return an error if there has been a commit *after* the draft was created/updated" in {

        val drafts = new MockItemDrafts {}
        drafts.mockCommitService.findCommitsSince(gwensDraft.src.data.id, gwensDraft.committed.getOrElse(gwensDraft.created)) returns {
          Seq(
            ItemCommit(ObjectId.get, gwensDraft.src.data.id, gwensDraft.src.data.id, gwen))
        }

        drafts.commit(gwen)(gwensDraft, false) match {
          case Failure(CommitsAfterDraft(commits)) => {
            commits.length === 1
          }
          case _ => failure("should have returned CommitsAfterDraft")
        }
      }

      "not delete the draft" in {
        val drafts = new MockItemDrafts {}
        drafts.commit(gwen)(gwensDraft, false)
        there was no(drafts.draftService).remove(gwensDraft)
      }

      class publishedScope(isItemPublished: Boolean, isDraftPublished: Boolean) extends Scope {
        val drafts = new MockItemDrafts {}
        drafts.itemService.isPublished(any[VersionedId[ObjectId]]) returns isItemPublished
        val item = mkItem(isDraftPublished)
        val draft = mkDraft(gwen, item)
        lazy val commit = drafts.commit(gwen)(draft).toOption.get
      }

      def runAssertions(
        isItemPublished: Boolean,
        isDraftPublished: Boolean,
        expectedId: VersionedId[ObjectId] => VersionedId[ObjectId],
        count: Int) = {

        def callCount[T <: AnyRef]: (T) => T = count match {
          case 0 => no[T]
          case 1 => one[T]
          case _ => throw new RuntimeException("Not supported")
        }

        s"when the item.published=$isItemPublished and draft.item.published=$isDraftPublished" should {

          s"commit the correct id" in new publishedScope(isItemPublished, isDraftPublished) {
            commit.srcId must_== item.id
            commit.committedId must_== expectedId(item.id)

            there was callCount[ItemDraftService](drafts.draftService).save {
              gwensDraft.copy(
                src = gwensDraft.src.copy(data = gwensDraft.src.data.copy(id = commit.committedId)),
                committed = any[Option[DateTime]])
            }
          }
        }
      }

      runAssertions(false, false, id => id, 1)
      runAssertions(false, true, id => bump(id), 1)
      runAssertions(true, false, id => bump(id), 1)
      runAssertions(true, true, id => bump(id), 1)
    }

    "when 2 people are committing" should {

      "not allow ed to save gwen's commit" in new Scope with MockItemDrafts {
        commit(ed)(gwensDraft) must_== Failure(UserCantCommit(ed, gwen))
      }

      "allow gwen to save gwen's commit" in new Scope with MockItemDrafts {
        commit(gwen)(gwensDraft) match {
          case Success(ItemCommit(_, srcId, newId, user, _)) => {
            srcId must_== item.id
            user must_== gwen
            there was one(assets).copyDraftToItem(gwensDraft.id, item.id)
            there was one(itemService).save(any[Item], any[Boolean])
          }
          case _ => failure("should have got an item commit")
        }
      }
    }

    "clone" should {

      class c(draft: Option[ItemDraft] = None,
        saveOk: Boolean = false,
        itemServiceSaveOk: Boolean = false) extends Scope with MockItemDrafts {
        val oid = ObjectId.get
        mockDraftService.load(any[ObjectId]) returns draft
        mockDraftService.save(any[ItemDraft]) returns mockWriteResult(saveOk)
        mockItemService.save(any[Item], any[Boolean]).answers { (obj: Any, mock: Any) =>
          val arr = obj.asInstanceOf[Array[Any]]
          if (itemServiceSaveOk) {
            Right(arr(0).asInstanceOf[Item].id)
          } else {
            Left("Mock Error")
          }
        }
      }

      "return LoadDraftFailed" in new c {
        clone(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
      }

      val mockDraft = ItemDraft(item, ed)

      "return SaveDraftFailed" in new c(Some(mockDraft)) {
        clone(ed)(oid) must_== Failure(SaveDraftFailed("Mock Error"))
      }

      "return CreateDraftFailed" in new c(draft = Some(mockDraft), itemServiceSaveOk = true) {
        clone(ed)(oid) match {
          case Failure(CreateDraftFailed(_)) => success
          case _ => failure("should have had a failure")
        }
      }

      "return DraftCloneResult" in new c(
        draft = Some(mockDraft),
        saveOk = true,
        itemServiceSaveOk = true) {
        clone(ed)(oid) match {
          case Success(DraftCloneResult(vid, id)) => success
          case _ => failure("should have had a failure")
        }
      }
    }
  }
}
