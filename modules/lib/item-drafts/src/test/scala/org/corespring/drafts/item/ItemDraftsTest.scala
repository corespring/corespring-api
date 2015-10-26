package org.corespring.drafts.item

import com.mongodb.CommandResult
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftDbUtils, ItemDraftService }
import org.corespring.models.auth.Permission
import org.corespring.models.item._
import org.corespring.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.salat.config.SalatContext
import org.corespring.services.item.ItemService
import org.corespring.services.{ OrgCollectionService, OrganizationService }
import org.corespring.test.fakes.Fakes.withMockCollection
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success, Validation }

class ItemDraftsTest extends Specification with Mockito {

  val collectionId = ObjectId.get
  val itemId = VersionedId(ObjectId.get, Some(0))
  val ed = OrgAndUser(SimpleOrg(ObjectId.get, "ed-org"), None)
  val gwen = OrgAndUser(SimpleOrg(ObjectId.get, "gwen-org"), None)
  val item = Item(id = itemId, collectionId = collectionId.toString)

  import scala.language.reflectiveCalls

  val utils = new ItemDraftDbUtils {
    override implicit def context: Context = new SalatContext(this.getClass.getClassLoader)
    def convertToDbo(id: DraftId) = idToDbo(id)
  }

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

  trait scope extends Scope {

    def mkDraftId(itemId: VersionedId[ObjectId], user: OrgAndUser) = DraftId(itemId.id, user.user.map(_.userName).getOrElse("test_user"), user.org.id)

    val itemService = {
      val m = mock[ItemService]
      m.save(any[Item], any[Boolean]).answers {
        (obj, mock) =>
          val arr: Array[Any] = obj.asInstanceOf[Array[Any]]
          val createNewVersion = arr(1).asInstanceOf[Boolean]
          Success(if (createNewVersion) bump(itemId) else itemId)
      }
      m.findOneById(any[VersionedId[ObjectId]]) returns Some(item)
      m.collectionIdForItem(any[VersionedId[ObjectId]]) returns Some(collectionId)
      m
    }

    val draftService = {
      val m = mock[ItemDraftService]
      m.save(any[ItemDraft]) returns mockWriteResult()
      m.owns(any[OrgAndUser], any[DraftId]) returns true
      m.removeByItemId(any[ObjectId]) returns true
      m
    }

    val orgService = {
      val m = mock[OrganizationService]
      m

    }

    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.getPermission(any[ObjectId], any[ObjectId]) returns Some(Permission.Write)
      m
    }

    val assets = {
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
      m.deleteDraftsByItemId(any[ObjectId]) returns Success()
      m
    }

    val commitService = {
      val m = mock[CommitService]
      m.save(any[ItemCommit]) returns mockWriteResult()
      m
    }

    implicit val context = new SalatContext(this.getClass.getClassLoader)

    private def mkDrafts() = new ItemDrafts(itemService,
      orgService,
      orgCollectionService,
      draftService,
      commitService,
      assets,
      context)

    lazy val itemDrafts = mkDrafts()

  }

  def mkItem(isPublished: Boolean) = Item(id = itemId, collectionId = collectionId.toString, published = isPublished)
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

    "removeByItemId" should {

      class removeByItemId extends scope {

      }

      "fail if orgCollectionService.getPermission returns None" in new removeByItemId {
        orgCollectionService.getPermission(any[ObjectId], any[ObjectId]) returns None
        itemDrafts.removeByItemId(ed)(itemId.id) must_== Failure(UserCantDeleteMultipleDrafts(ed, itemId.id))
      }

      "fail if orgCollectionService.getPermission returns Some(Read)" in new removeByItemId {
        orgCollectionService.getPermission(any[ObjectId], any[ObjectId]) returns Some(Permission.Read)
        itemDrafts.removeByItemId(ed)(itemId.id) must_== Failure(UserCantDeleteMultipleDrafts(ed, itemId.id))
      }

      "fail if itemService.collectionIdForItem returns None" in new removeByItemId {
        itemService.collectionIdForItem(any[VersionedId[ObjectId]]) returns None
        itemDrafts.removeByItemId(ed)(itemId.id) must_== Failure(UserCantDeleteMultipleDrafts(ed, itemId.id))
      }

      "return the id when successful" in new removeByItemId {
        itemDrafts.removeByItemId(ed)(itemId.id) must_== Success(itemId.id)
      }

    }
    "remove" should {

      class remove(removeSuccessful: Boolean, owns: Boolean, assetsSuccessful: Boolean) extends scope {
        draftService.remove(any[DraftId]) returns removeSuccessful
        draftService.owns(any[OrgAndUser], any[DraftId]) returns owns
        assets.deleteDraft(any[DraftId]) returns {
          if (assetsSuccessful) Success(Unit) else Failure(TestError("deleteDraft"))
        }
      }

      "fail if user doesn't own draft" in new remove(true, false, false) {
        itemDrafts.remove(gwen)(oid) must_== Failure(UserCantRemove(gwen, oid))
      }

      "fail if remove failed" in new remove(false, true, false) {
        itemDrafts.remove(ed)(oid) must_== Failure(DeleteDraftFailed(oid))
      }

      "fail if assets.deleteDraft failed" in new remove(true, true, false) {
        itemDrafts.remove(ed)(oid) must_== Failure(TestError("deleteDraft"))
      }
      "succeed" in new remove(true, true, true) {
        itemDrafts.remove(ed)(oid) must_== Success(oid)
      }
    }

    "load" should {

      class load(
        owns: Boolean, load: Boolean) extends scope {

        val draft = mkDraft(ed, item)
        draftService.owns(any[OrgAndUser], any[DraftId]) returns {
          owns
        }
        draftService.load(any[DraftId]) returns {
          if (load) Some(draft) else None
        }
      }

      "fail if user doesn't own draft" in new load(false, false) {
        itemDrafts.load(ed)(oid) must_== Failure(UserCantLoad(ed, oid))
      }

      "fail if service.load fails" in new load(true, false) {
        itemDrafts.load(ed)(oid) must_== Failure(LoadDraftFailed(oid.toString))
      }

      "succeed" in new load(true, true) {
        itemDrafts.load(ed)(oid) must_== Success(draft)
      }
    }

    "cloneDraft" should {

      class cloneDraft(
        loadResult: Validation[DraftError, ItemDraft] = Failure(TestError("load")),
        createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")),
        saveSuccess: Boolean = false) extends scope {

        //we are overriding some draft methods here.
        override lazy val itemDrafts = new ItemDrafts(itemService, orgService, orgCollectionService, draftService, commitService, assets, context) {
          override def load(user: OrgAndUser)(id: DraftId) = loadResult
          override def create(id: DraftId, user: OrgAndUser, expires: Option[DateTime]) = createResult
        }

        val draft = mkDraft(ed, item)
        itemService.save(any[Item], any[Boolean]) returns {
          if (saveSuccess) Success(itemId) else Failure(org.corespring.services.errors.GeneralError("Err", None))
        }
      }

      "fail if load fails" in new cloneDraft() {
        itemDrafts.cloneDraft(ed)(oid) must_== Failure(TestError("load"))
      }

      "fail if itemService.save" in new cloneDraft(Success(mkDraft(ed, item))) {
        itemDrafts.cloneDraft(ed)(oid) must_== Failure(SaveDraftFailed("Err"))
      }

      "fail if create fails" in new cloneDraft(Success(mkDraft(ed, item)), saveSuccess = true) {
        itemDrafts.cloneDraft(ed)(oid) must_== Failure(TestError("create"))
      }

      "succeed" in new cloneDraft(Success(mkDraft(ed, item)), Success(mkDraft(ed, item)), true) {
        itemDrafts.cloneDraft(ed)(oid) must_== Success(DraftCloneResult(itemId, oid))
      }
    }

    "create" should {
      class create(
        canCreate: Boolean,
        val getUnpublishedVersion: Option[Item] = None,
        copyResult: Validation[DraftError, DraftId] = Failure(TestError("copyAssets")),
        saveResult: Validation[DraftError, DraftId] = Failure(TestError("save"))) extends scope {

        override lazy val itemDrafts = new ItemDrafts(itemService, orgService, orgCollectionService, draftService, commitService, assets, context) {
          override def save(u: OrgAndUser)(d: ItemDraft) = saveResult
          override def userCanCreateDraft(id: ObjectId, user: OrgAndUser): Boolean = canCreate
        }

        itemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion
        assets.copyItemToDraft(any[VersionedId[ObjectId]], any[DraftId]) returns copyResult

      }

      "fail if itemService.collectionIdForItem returns none" in new scope {
        itemService.collectionIdForItem(any[VersionedId[ObjectId]]) returns None
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(UserCantCreate(ed, itemId.id))
      }

      "fail if orgCollectionService.getPermission returns none" in new scope {
        orgCollectionService.getPermission(any[ObjectId], any[ObjectId]) returns None
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(UserCantCreate(ed, itemId.id))
      }

      "fail if userCanCreateDraft fails" in new create(false) {
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(UserCantCreate(ed, itemId.id))
      }

      "fail if itemService.getOrCreateUnpublishedVersion fails" in new create(true) {
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(GetUnpublishedItemError(itemId.id))
      }

      "fail if assets.copyItemToDraft fails" in new create(true, Some(item)) {
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(TestError("copyAssets"))
      }

      "fail if save fails" in new create(true, Some(item), Success(oid)) {
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) must_== Failure(TestError("save"))
      }

      "succeed" in new create(true, Some(item), Success(oid), Success(oid)) {
        itemDrafts.create(mkDraftId(itemId, ed), ed, None) match {
          case Success(d) => {
            d.parent.data must_== getUnpublishedVersion.get
          }
          case _ => failure("should have been successful")
        }
      }
    }

    "loadOrCreate" should {

      class loadOrCreate(
        val load: Option[ItemDraft] = None,
        val getUnpublishedVersion: Option[Item] = None,
        val createResult: Validation[DraftError, ItemDraft] = Failure(TestError("create")))
        extends scope {
        draftService.load(any[DraftId]) returns load
        itemService.getOrCreateUnpublishedVersion(any[VersionedId[ObjectId]]) returns getUnpublishedVersion

        override lazy val itemDrafts = new ItemDrafts(itemService, orgService, orgCollectionService, draftService, commitService, assets, context) {
          override def create(id: DraftId, user: OrgAndUser, expires: Option[DateTime] = None) = createResult
        }
      }

      "fail if load fails and create fails" in new loadOrCreate() {
        itemDrafts.loadOrCreate(ed)(oid) must_== Failure(TestError("create"))
      }

      "fail if the draft.parent is out of date and the draft.parent != draft.change" in new loadOrCreate(
        Some(mkDraft(ed, item, item.copy(playerDefinition = Some(PlayerDefinition("Change!"))))),
        Some(item.cloneItem)) {
        itemDrafts.loadOrCreate(ed)(oid) match {
          case Success(draft) => failure("should have failed")
          case Failure(ItemDraftIsOutOfDate(d, i)) => {
            d must_== load.get
            i must_== ItemSrc(getUnpublishedVersion.get)
          }
          case _ => failure("should have been an out of date error")
        }
      }

      "return a new item if the draft isn't found" in new loadOrCreate(
        None,
        None,
        Success(mkDraft(ed, item))) {
        itemDrafts.loadOrCreate(ed)(oid) match {
          case Success(draft) => draft must_== createResult.toOption.get
          case Failure(ItemDraftIsOutOfDate(d, i)) => failure("should have been successful")
          case _ => failure("should have been an out of date error")
        }
      }

      "create a new draft from the item if draft isn't found" in new loadOrCreate(
        None,
        Some(item),
        Success(mkDraft(ed, mkItemWithXhtml("created")))) {
        itemDrafts.loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.parent.data.playerDefinition.map(_.xhtml) must_== Some("created")
          case Failure(e) => {
            println("error --->")
            println(e)
            failure("should have been successful")
          }
        }
      }

      "create a new draft from the item if the loaded draft doesn't have changes" in new loadOrCreate(
        Some(mkDraft(ed, item)),
        Some(item),
        Success(mkDraft(ed, mkItemWithXhtml("created")))) {
        itemDrafts.loadOrCreate(ed)(oid) match {
          case Success(draft) => draft.parent.data.playerDefinition.map(_.xhtml) must_== Some("created")
          case Failure(e) => {
            println("error --->")
            println(e)
            failure("should have been successful")
          }
        }
      }

      "return the draft if found and has local changes" in new loadOrCreate(
        Some(mkDraft(ed, item, mkItemWithXhtml("change"))),
        Some(item)) {
        itemDrafts.loadOrCreate(ed)(oid) match {
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

      class save(owns: Boolean = false, saveDraft: Boolean = false) extends scope {
        draftService.owns(any[OrgAndUser], any[DraftId]) returns owns
        draftService.save(any[ItemDraft]) returns mockWriteResult(saveDraft)
      }

      "fail if user doesn't own" in new save() {
        itemDrafts.save(ed)(mkDraft(gwen, item)) must_== Failure(UserCantSave(ed, gwen))
      }

      "fail if save fails" in new save(true) {
        itemDrafts.save(ed)(mkDraft(ed, item)) must_== Failure(SaveDataFailed(mockWriteResult(false).getLastError.getErrorMessage))
      }

      "succeed" in new save(true, true) {
        itemDrafts.save(ed)(mkDraft(ed, item)) must_== Success(oid)
      }
    }

    "hasSrcChanged" should {
      class hasSrcChanged extends scope {
        val item1 = Item(id = itemId, collectionId = collectionId.toString)
      }

      "return false if item has not changed" in new hasSrcChanged {
        val item2 = item1.copy()
        itemDrafts.hasSrcChanged(item1, item2) must_== false
      }

      "return true if collectionId has changed" in new hasSrcChanged {
        val item2 = item1.copy(collectionId = "1234")
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if taskInfo has changed" in new hasSrcChanged {
        val item2 = item1.copy(taskInfo = Some(TaskInfo()))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if playerDefinition has changed" in new hasSrcChanged {
        val item2 = item1.copy(playerDefinition = Some(PlayerDefinition("")))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if supportingMaterials has changed" in new hasSrcChanged {
        val item2 = item1.copy(supportingMaterials = Seq(Resource(name = "test", files = Seq.empty)))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if standards has changed" in new hasSrcChanged {
        val item2 = item1.copy(standards = Seq("std1"))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if reviewsPassed has changed" in new hasSrcChanged {
        val item2 = item1.copy(reviewsPassed = Seq("rp1"))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if reviewsPassedOther has changed" in new hasSrcChanged {
        val item2 = item1.copy(reviewsPassedOther = Some("rpo1"))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if otherAlignments has changed" in new hasSrcChanged {
        val item2 = item1.copy(otherAlignments = Some(Alignments()))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if contributorDetails has changed" in new hasSrcChanged {
        val item2 = item1.copy(contributorDetails = Some(ContributorDetails()))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if priorUse has changed" in new hasSrcChanged {
        val item2 = item1.copy(priorUse = Some(""))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if priorUseOther has changed" in new hasSrcChanged {
        val item2 = item1.copy(priorUseOther = Some(""))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }

      "return true if priorGradeLevels has changed" in new hasSrcChanged {
        val item2 = item1.copy(priorGradeLevels = Seq(""))
        itemDrafts.hasSrcChanged(item1, item2) must_== true
      }
    }

    "addFileToChangeSet" should {

      class addFileToChangeSet(n: Int = 1) extends scope with withMockCollection {
        draftService.collection returns mockCollection
      }

      "update the document in the db" in new addFileToChangeSet {
        val draft = mkDraft(ed, item)
        val file = StoredFile("test.png", "image/png", false)
        itemDrafts.addFileToChangeSet(draft, file)
        val expectedQuery = utils.convertToDbo(draft.id)
        val (q, u) = captureUpdate
        q.value === expectedQuery
        val fileDbo = com.novus.salat.grater[StoredFile].asDBObject(file)
        val expectedUpdate = MongoDBObject("$addToSet" -> MongoDBObject("change.data.playerDefinition.files" -> fileDbo))
        u.value === expectedUpdate

      }
    }
  }

}
