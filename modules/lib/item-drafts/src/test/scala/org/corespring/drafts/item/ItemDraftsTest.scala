package org.corespring.drafts.item

import java.util.regex.MatchResult

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.mockito.InOrder
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Order, Success, Failure }

class ItemDraftsTest extends Specification with Mockito {

  private trait TestDrafts extends ItemDrafts {
    override def itemService: ItemService = ???

    override def draftService: ItemDraftService = ???

    override def assets: ItemDraftAssets = ???

    override def commitService: CommitService = ???

    override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = true

  }

  val itemId = VersionedId(ObjectId.get, Some(0))
  val ed = OrgAndUser(SimpleOrg(ObjectId.get, "ed-org"), None)
  val gwen = OrgAndUser(SimpleOrg(ObjectId.get, "gwen-org"), None)
  val item = Item(id = itemId)

  trait OrgsAndItems extends Scope {

  }

  "ItemDrafts" should {

    "when creating" should {

      class scp(canCreate: Boolean) extends OrgsAndItems {
        val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item), gwen)
        lazy val drafts = new TestDrafts {

          override protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean = canCreate

          override val itemService: ItemService = {
            mock[ItemService].findOneById(any[VersionedId[ObjectId]]) returns Some(item)
          }

          override val assets: ItemDraftAssets = mock[ItemDraftAssets].copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]) answers { (obj, mock) =>
            val arr = obj.asInstanceOf[Array[Any]]
            Success(arr(1).asInstanceOf[ObjectId])
          }

          override val draftService: ItemDraftService = mock[ItemDraftService].save(any[ItemDraft]) returns {
            mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
          }

        }
      }

      "return error if create" in new scp(false) {
        drafts.create(VersionedId(ObjectId.get), ed) must_== None
      }

      "return the item draft" in new scp(true) {
        drafts.create(VersionedId(ObjectId.get), ed) match {
          case Some(ItemDraft(_, _, _, _, _)) => success
          case _ => failure("should have got an item draft")
        }
      }

      "always uses the latest version of an item as the src for a draft" in new scp(true) {
        drafts.create(itemId, ed)
        there was one(drafts.itemService).findOneById(itemId.copy(version = None))
      }
    }

    "when saving" should {

      class scp extends OrgsAndItems {
        val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item), gwen)
        lazy val drafts = new TestDrafts {
          override def draftService: ItemDraftService = mock[ItemDraftService].save(any[ItemDraft]) returns {
            mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
          }
        }
      }

      "not allow ed to save gwen's draft" in new scp {
        drafts.save(ed)(gwensDraft) must_== Failure(UserCantSave(ed, gwen))
      }

      "allow gwen to save gwen's draft" in new scp {
        drafts.save(gwen)(gwensDraft) must_== Success(gwensDraft.id)
      }
    }

    "when removing a draft" should {

      class scp(n: Int) extends OrgsAndItems {
        val oid = ObjectId.get
        lazy val drafts = new TestDrafts {
          override val draftService: ItemDraftService = mock[ItemDraftService]
            .removeDraftByIdAndUser(any[ObjectId], any[OrgAndUser])
            .returns(mock[WriteResult].getN returns n)

          override val assets: ItemDraftAssets = mock[ItemDraftAssets].deleteDraft(any[ObjectId]) returns Success(Unit)
        }
      }

      "return delete failed" in new scp(0) {
        drafts.removeDraftByIdAndUser(oid, ed) must_== Failure(DeleteFailed)
      }

      "return draft id" in new scp(1) {
        drafts.removeDraftByIdAndUser(oid, ed) must_== Success(oid)
        there was one(drafts.assets).deleteDraft(oid)
      }
    }

    "when committing" should {

      trait scp extends OrgsAndItems {
        lazy val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item), gwen)
        val mockCommitService: CommitService = {
          val m = mock[CommitService]
          m.save(any[ItemCommit]).returns(
            mock[WriteResult].getLastError.returns {
              mock[CommandResult].ok returns true
            })
          m.findByIdAndVersion(any[ObjectId], any[Long]) returns Seq.empty
        }

        lazy val mockItemService: ItemService = mock[ItemService].save(any[Item], any[Boolean]) returns Right(item.id)

        lazy val drafts = new TestDrafts {
          override val draftService: ItemDraftService = {
            val m = mock[ItemDraftService]
            m.listForOrg(any[ObjectId]).returns(Seq.empty)
            m.remove(any[ItemDraft]).returns(true)
            m.save(any[ItemDraft]).returns {
              mock[WriteResult].getLastError.returns(mock[CommandResult].ok returns true)
            }
          }

          override val commitService: CommitService = mockCommitService

          override val itemService: ItemService = mockItemService

          override val assets: ItemDraftAssets = {
            val m = mock[ItemDraftAssets]
            m.copyDraftToItem(any[ObjectId], any[VersionedId[ObjectId]]) answers { (arr, mock) =>
              val array = arr.asInstanceOf[Array[Any]]
              Success(array(1).asInstanceOf[VersionedId[ObjectId]])
            }
            m.deleteDraft(any[ObjectId]) answers { (id) => Success(id.asInstanceOf[ObjectId]) }
          }
        }
      }

      "not delete the draft" in new scp {
        drafts.commit(gwen)(gwensDraft, false)
        there was no(drafts.draftService).remove(gwensDraft)
      }

      "return an error if another draft exists that has committed"

      def bump(vid: VersionedId[ObjectId]): VersionedId[ObjectId] = vid.copy(version = vid.version.map { n => n + 1 })

      class publishedScp(isItemPublished: Boolean, isDraftPublished: Boolean) extends scp {
        val item = Item(id = itemId, published = isDraftPublished)
        override lazy val gwensDraft = ItemDraft(ObjectId.get, ItemSrc(item), gwen)

        override lazy val mockItemService: ItemService = {
          val m = mock[ItemService]

          m.isPublished(any[VersionedId[ObjectId]]) returns isItemPublished
          m.save(any[Item], any[Boolean]) answers { (obj, mock) =>
            val arr: Array[Any] = obj.asInstanceOf[Array[Any]]
            val createNewVersion = arr(1).asInstanceOf[Boolean]
            Right(
              if (createNewVersion) bump(itemId) else itemId)
          }
        }
        lazy val commit = drafts.commit(gwen)(gwensDraft).toOption.get
      }

      def rnAssertions(isItemPublished: Boolean,
        isDraftPublished: Boolean,
        expectedCommittedId: VersionedId[ObjectId] => VersionedId[ObjectId],
        count: Int) = {

        def callCount[T <: AnyRef]: (T) => T = count match {
          case 0 => no[T]
          case 1 => one[T]
          case _ => throw new RuntimeException("Not supported")
        }

        s"when the item.published=$isItemPublished and draft.item.published=$isDraftPublished" should {

          s"commit the correct id" in new publishedScp(isItemPublished, isDraftPublished) {
            commit.srcId must_== item.id
            commit.committedId must_== expectedCommittedId(item.id)
          }

          s"call draftService.save $count times" in new publishedScp(isItemPublished, isDraftPublished) {
            val update = gwensDraft.copy(src = gwensDraft.src.copy(data = gwensDraft.src.data.copy(id = commit.committedId)))
            there was callCount[ItemDraftService](drafts.draftService).save(update)
          }
        }
      }

      rnAssertions(false, false, id => id, 0)
      rnAssertions(false, true, id => bump(id), 1)
      rnAssertions(true, false, id => bump(id), 1)
      rnAssertions(true, true, id => bump(id), 1)

      "when 2 users are trying to commit" should {

        "not allow ed to save gwen's commit" in new scp {
          drafts.commit(ed)(gwensDraft) must_== Failure(UserCantCommit(ed, gwen))
        }

        "allow gwen to save gwen's commit" in new scp {
          drafts.commit(gwen)(gwensDraft) match {
            case Success(ItemCommit(_, srcId, newId, user, _)) => {
              srcId must_== item.id
              user must_== gwen
              there was one(drafts.assets).copyDraftToItem(gwensDraft.id, item.id)
              there was one(drafts.itemService).save(item.copy(id = item.id.copy(version = None)), false)
            }
            case _ => failure("should have got an item commit")
          }
        }
      }

      "clone" should {

        class scp(
          draft: Option[ItemDraft] = None,
          saveOk: Boolean = false,
          itemServiceSaveOk: Boolean = false) extends OrgsAndItems {
          val oid = ObjectId.get
          lazy val drafts = new TestDrafts {

            override val draftService: ItemDraftService = {
              val m = mock[ItemDraftService]
              m.load(any[ObjectId]).returns(draft)
              m.save(any[ItemDraft]).returns {
                mock[WriteResult].getLastError.returns {
                  mock[CommandResult].ok.returns(saveOk)
                }
              }
            }

            override val assets: ItemDraftAssets = {
              mock[ItemDraftAssets]
                .copyItemToDraft(any[VersionedId[ObjectId]], any[ObjectId]).answers { (obj, mock) =>
                  val arr = obj.asInstanceOf[Array[Any]]
                  Success(arr(1).asInstanceOf[ObjectId])
                }
            }

            override val itemService: ItemService = {
              val m = mock[ItemService]

              m.save(any[Item], any[Boolean]).answers { (obj, mock) =>
                var arr = obj.asInstanceOf[Array[Any]]
                if (itemServiceSaveOk) {
                  Right(arr(0).asInstanceOf[Item].id)
                } else {
                  Left("Mock Error")
                }
              }

              m.findOneById(any[VersionedId[ObjectId]]).returns(Some(item))
            }
          }
        }

        "return LoadDraftFailed" in new scp {
          drafts.clone(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
        }

        val mockDraft = ItemDraft(item, ed)

        "return SaveDraftFailed" in new scp(Some(mockDraft)) {
          drafts.clone(ed)(oid) must_== Failure(SaveDraftFailed("Mock Error"))
        }

        "return CreateDraftFailed" in new scp(draft = Some(mockDraft), itemServiceSaveOk = true) {
          drafts.clone(ed)(oid) match {
            case Failure(CreateDraftFailed(_)) => success
            case _ => failure("should have had a failure")
          }
        }

        "return DraftCloneResult" in new scp(
          draft = Some(mockDraft),
          saveOk = true,
          itemServiceSaveOk = true) {
          drafts.clone(ed)(oid) match {
            case Success(DraftCloneResult(vid, id)) => success
            case _ => failure("should have had a failure")
          }
        }
      }
    }
  }
}
