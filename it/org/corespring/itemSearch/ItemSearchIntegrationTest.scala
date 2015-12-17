package org.corespring.itemSearch

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.helpers.{ StandardHelper, ItemHelper }
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import org.corespring.it.{ IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.item.{StandardCluster, TaskInfo, Item, PlayerDefinition}
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{ Success, Failure }

class ItemSearchIntegrationTest extends IntegrationSpecification {

  trait scope
    extends orgWithAccessTokenAndItem
    with ItemIndexCleaner {

    cleanIndex()

    val itemIndexService = main.itemIndexService

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

    protected def search(text: Option[String]) = {
      val query = ItemIndexQuery(text = text)
      val futureResult = itemIndexService.search(query)
      Await.ready(futureResult, Duration.Inf).value.get.get
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

  "search" should {

    "find item by standardCluster" in new scope {

      search(Some("test-cluster")) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(v) => {
          v.total should_== 1
          hitsToSet(v.hits) should_== idsToSet(itemWithClusterId)
        }
      }
    }

  }
}
