package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.{ ItemAuthorizationError, CollectionAuthorizationError }
import org.specs2.specification.{ After }

import scalaz.{ Success, Failure, Validation }

class OrgItemSharingServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends After with InsertionHelper {
    val service = services.orgItemSharingService

    val otherOrg = services.orgService.insert(Organization("other-org"), None).toOption.get
    val rootOrg = services.orgService.insert(Organization("root-org"), None).toOption.get
    val childOrg = services.orgService.insert(Organization("child-org"), Some(rootOrg.id)).toOption.get
    val publicOrg = services.orgService.insert(Organization("public-org"), None).toOption.get

    val writableCollectionWithItem = insertCollection("writable-with-item", rootOrg)
    val writableCollection = insertCollection("writable", rootOrg)
    val readableCollection = insertCollection("readable", otherOrg)
    giveOrgAccess(rootOrg, readableCollection, Permission.Read)

    val item = Item(
      collectionId = writableCollectionWithItem.id.toString,
      taskInfo = Some(TaskInfo(title = Some("title"))),
      standards = Seq("S1", "S2"))
    val itemId = services.itemService.insert(item).get

    override def after: Any = removeAllData()

    def authorizationError[R](p: Permission, colls: ContentCollection*): Validation[CollectionAuthorizationError, R] = {
      Failure(CollectionAuthorizationError(rootOrg.id, p, colls.map(_.id): _*))
    }

    def itemAuthorizationError(orgId: ObjectId, p: Permission, itemIds: VersionedId[ObjectId]*) = {
      Failure(ItemAuthorizationError(orgId, p, itemIds: _*))
    }
  }

  "unShareItems" should {

    "remove shared item from collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id)
      service.unShareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
      service.isItemSharedWith(item.id, writableCollection.id) must_== false
    }

    "return error when org does not have write permissions for all collections" in new scope {
      service.unShareItems(rootOrg.id, Seq(item.id), Seq(readableCollection.id)) must_== Failure(_: CollectionAuthorizationError)
    }
  }

  "shareItems" should {

    "add the item to collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
      service.isItemSharedWith(item.id, writableCollection.id) must_== true
    }

    "return error when org cannot write into collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), readableCollection.id) must_== authorizationError(Permission.Write, readableCollection)
    }

    "return error when org cannot write for all items" in new scope {
      service.shareItems(childOrg.id, Seq(item.id), readableCollection.id) must_== itemAuthorizationError(childOrg.id, Permission.Read, item.id)
    }
  }
}
