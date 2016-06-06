package org.corespring.itemSearch

import org.bson.types.ObjectId
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import org.corespring.it.{ IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.item.{ Item, StandardCluster, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Await
import scala.concurrent.duration._

class ItemSearchIntegrationTest extends IntegrationSpecification {

  trait scope
    extends orgWithAccessTokenAndItem
    with ItemIndexCleaner {

    cleanIndex()

    val itemIndexService = main.itemIndexService
    val itemService = main.itemService

    val itemWithClusterId = insertItemWithStandardCluster("test-cluster")

    override def after = {
      logger.info("after.. cleaning up..")
      removeData()
      cleanIndex()
    }

    protected def insertItemWithStandardCluster(cluster: String) = {
      ItemHelper.create(collectionId, itemWithStandardCluster(cluster))
    }

    protected def itemWithStandardCluster(c: String): Item = {
      Item(collectionId = collectionId.toString,
        taskInfo = Some(TaskInfo(standardClusters = Seq(StandardCluster(c, false, "manual")))))
    }

    protected def search(text: Option[String]): ItemIndexSearchResult = {
      val query = ItemIndexQuery(text = text, collections = Seq(collectionId.toString))
      val futureResult = itemIndexService.search(query)
      Await.result(futureResult, Duration.Inf).toOption.get
    }

    protected def hitsShouldContain(results: ItemIndexSearchResult, id: VersionedId[ObjectId]*) = {
      val resultIds = results.hits.map(_.id).toSet
      val expectedIds = id.map(_.toString).toSet
      resultIds should_== expectedIds
    }

    protected def hitsToSet(hits: Seq[ItemIndexHit]) = {
      hits.map(_.id).toSet
    }

    protected def idsToSet(id: VersionedId[ObjectId]*) = {
      id.map(_.toString).toSet
    }
  }

  trait indexWithTwoItems extends scope {
    val item = Item(
      collectionId = collectionId.toString,
      taskInfo = Some(TaskInfo(title = Some("test item"))))
    ItemHelper.create(collectionId, item)
    itemService.publish(item.id)
    val unpublishedItem = itemService.getOrCreateUnpublishedVersion(item.id).get
    require(unpublishedItem.id.version.map(_ - 1) == item.id.version)

    def text: String
    def published: Option[Boolean] = None

    lazy val result = {
      val query = ItemIndexQuery(
        text = Some(text),
        collections = Seq(collectionId.toString),
        published = published)
      Await.result(itemIndexService.search(query), 5.seconds).toOption.get
    }
  }

  trait indexWithOnePublishedItem extends scope {

  }

  trait indexThreeItems extends scope {

  }

  "indexing" should {

    "remove old versions from the index" in new indexThreeItems {
      pending
    }
  }

  "search" should {

    "by cluster" should {
      "find item by standardCluster" in new scope {

        val v = search(Some("test-cluster"))
        v.total should_== 1
        hitsToSet(v.hits) should_== idsToSet(itemWithClusterId)
      }
    }

    trait byTitle extends indexWithTwoItems {
      val text = "test item"
    }

    trait byObjectId extends indexWithTwoItems {
      val text = item.id.id.toString
    }

    trait byVersionedId extends indexWithTwoItems {
      val text = item.id.toString
    }

    "by Text in title" should {
      "return the latest version of the item if published is undefined" in new byTitle {
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "return the penultimate version of the item if published: true" in new byTitle {
        override val published = Some(true)
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return the latest version of the item if published: false" in new byTitle {
        override val published = Some(false)
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }
    }

    "by ObjectId" should {

      "return the latest version of the item if published is undefined" in new byObjectId {
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "return the penultimate version of the item if published: true" in new byObjectId {
        override val published = Some(true)
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return the latest version of the item if published: false" in new byObjectId {
        override val published = Some(false)
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

    }

    "by VersionedId" should {
      "return 1 document" in new byVersionedId {
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return 0 documents if published: false" in new byVersionedId {
        override val published = Some(false)
        result.total must_== 0
      }

      "return 1 documents if published: false" in new byVersionedId {
        override val published = Some(true)
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }
    }
  }
}

