package org.corespring.itemSearch

import java.net.URL

import org.bson.types.ObjectId
import org.corespring.elasticsearch.WSClient
import org.corespring.it.helpers.{ ItemHelper, StandardHelper }
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import org.corespring.it.{ IntegrationSpecification, ItemIndexCleaner }
import org.corespring.itemSearch.SearchMode.SearchMode
import org.corespring.models.Standard
import org.corespring.models.item.{ Item, StandardCluster, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Await
import scala.concurrent.duration.Duration
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
      val futureResult = itemIndexService.search(query, None)
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
    def mode: SearchMode = SearchMode.latest

    lazy val result = {
      val query = ItemIndexQuery(
        text = if (text.isEmpty) None else Some(text),
        collections = Seq(collectionId.toString),
        published = published,
        mode = mode)
      println("pause - allow index to prepare itself")
      Thread.sleep(1000)
      Await.result(itemIndexService.search(query, None), 5.seconds).toOption.get
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

    "by dotNotation" should {

      trait dotNotation
        extends orgWithAccessTokenAndItem
        with ItemIndexCleaner {

        logger.info(s"before - remove data and clean the index...")
        removeData()
        cleanIndex()
        logger.info(s"before - remove data and clean the index.. DONE.")

        val itemIndexService = main.itemIndexService
        val itemService = main.itemService

        val standards = (0 to 4).map { index =>

          val s = Standard(
            standard = Some(s"standard-$index"),
            dotNotation = Some(s"DN.AB.$index"),
            category = Some(s"category-$index"),
            subCategory = Some(s"subCategory-$index"),
            subject = Some(s"category-$index"))
          StandardHelper.create(s)
          s
        }

        val item = {
          val i = Item(collectionId = collectionId.toString, standards = standards.flatMap(_.dotNotation))
          itemService.insert(i)
          i
        }

        override def after = {
          logger.info("after.. cleaning up..")
          removeData()
          cleanIndex()
        }
      }

      "find 1 item by the starting term of the dotNotation" in new dotNotation {

        lazy val result = {
          val query = ItemIndexQuery(
            text = Some("DN"),
            collections = Seq(collectionId.toString))
          Await.result(itemIndexService.search(query, None), 5.seconds).toOption.get
        }

        result.total must_== 1
        result.hits(0).id must_== item.id.toString
      }

      "find 1 item by the middle term of the dotNotation" in new dotNotation {

        lazy val result = {
          val query = ItemIndexQuery(
            text = Some("AB"),
            collections = Seq(collectionId.toString))
          Await.result(itemIndexService.search(query, None), 5.seconds).toOption.get
        }

        result.total must_== 1
        result.hits(0).id must_== item.id.toString
      }
    }

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

    trait byPublishedVersionedId extends indexWithTwoItems {
      val text = item.id.toString
    }

    trait byUnpublishedVersionedId extends indexWithTwoItems {
      val text = unpublishedItem.id.toString
    }

    trait emptyQuery extends indexWithTwoItems {
      val text = ""
    }

    "by Text in title" should {
      "return the latest version of the item if published is undefined" in new byTitle {
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "text and title - return the penultimate version of the item" in new byTitle {
        override val published = None //Some(true)
        override val mode = SearchMode.latestPublished
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return the latest version of the item if published: false" in new byTitle {
        override val published = Some(false)
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }
    }

    "by empty query" should {

      "return the 2 latest items" in new emptyQuery {
        result.total must_== 2
        result.hits.map(_.id).toSet must_== Set(itemWithClusterId.toString, unpublishedItem.id.toString)
      }
    }

    "by ObjectId" should {

      "return the latest version of the item if published is undefined" in new byObjectId {
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "return the penultimate version of the item if published: true" in new byObjectId {
        override val published = None
        override val mode = SearchMode.latestPublished
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return the latest version of the item if published: false" in new byObjectId {
        override val published = Some(false)
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

    }

    "by published VersionedId" should {
      "return 1 document only" in new byPublishedVersionedId {
        override val mode = SearchMode.latestPublished
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }

      "return 0 documents if published: false" in new byPublishedVersionedId {
        override val published = Some(false)
        result.total must_== 0
      }

      "return 1 documents if published: true" in new byPublishedVersionedId {
        override val mode = SearchMode.latestPublished
        override val published = Some(true)
        result.total must_== 1
        result.hits.head.id must_== item.id.toString
      }
    }

    "by unpublished VersionedId" should {
      "return 1 document only" in new byUnpublishedVersionedId {
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "return 1 documents if published: false" in new byUnpublishedVersionedId {
        override val published = Some(false)
        result.total must_== 1
        result.hits.head.id must_== unpublishedItem.id.toString
      }

      "return 0 documents if published: true" in new byUnpublishedVersionedId {
        override val published = Some(true)
        result.total must_== 0
      }
    }
  }
}

