package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.{ ItemAuthorizationError, CollectionAuthorizationError }
import org.specs2.specification.{ After, Scope }

import scalaz.{ Failure, Validation }

class OrgItemSharingServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends After {
    val service = services.contentCollectionService

    val otherOrg = services.orgService.insert(Organization("other-org"), None).toOption.get
    val rootOrg = services.orgService.insert(Organization("root-org"), None).toOption.get
    val childOrg = services.orgService.insert(Organization("child-org"), Some(rootOrg.id)).toOption.get
    val publicOrg = services.orgService.insert(Organization("public-org"), None).toOption.get

    //collections added to rootOrg
    val readableCollection = ContentCollection("readable-col", otherOrg.id)
    service.insertCollection(readableCollection)
    //Give root org read access
    services.orgCollectionService.upsertAccessToCollection(rootOrg.id, readableCollection.id, Permission.Read)

    val writableCollection = ContentCollection("writable-col", rootOrg.id)
    service.insertCollection(writableCollection)

    val writableCollectionWithItem = ContentCollection("writable-with-item-col", rootOrg.id)
    service.insertCollection(writableCollectionWithItem)

    val defaultCollection = ContentCollection(ContentCollection.Default, rootOrg.id)
    service.insertCollection(defaultCollection)

    //collections added to childOrg
    val readableChildOrgCollection = ContentCollection("readable-child-org-col", otherOrg.id)
    service.insertCollection(readableChildOrgCollection)
    services.orgCollectionService.upsertAccessToCollection(childOrg.id, readableChildOrgCollection.id, Permission.Read)

    val writableChildOrgCollection = ContentCollection("writable-child-org-col", otherOrg.id)
    service.insertCollection(writableChildOrgCollection)
    services.orgCollectionService.upsertAccessToCollection(childOrg.id, writableChildOrgCollection.id, Permission.Write)

    //collection added to publicOrg
    val publicCollection = ContentCollection("public-org-col", publicOrg.id, isPublic = true)
    service.insertCollection(publicCollection)

    //rootOrg's writableCollectionWithItem contains one item
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

  "shareCollectionWithOrg" should {
    "share the collection with the org" in new scope {
      service.isAuthorized(childOrg.id, writableCollection.id, Permission.Read) must_== Failure(_: PlatformServiceError)
      service.shareCollectionWithOrg(writableCollection.id, childOrg.id, Permission.Read)
      service.isAuthorized(childOrg.id, writableCollection.id, Permission.Read) must_== Success()
    }
  }
}
