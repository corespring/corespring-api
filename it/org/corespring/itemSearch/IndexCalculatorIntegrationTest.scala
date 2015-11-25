package org.corespring.itemSearch
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.elasticsearch.ContentIndexer
import org.corespring.it.helpers._
import org.corespring.it.{ FieldValuesIniter, IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.item.Item
import org.corespring.services.salat.bootstrap.CollectionNames
import org.specs2.mutable.After
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scalaz._

class IndexCalculatorIntegrationTest extends IntegrationSpecification {

  object Config {
    val RetryCount = 10
    val RetryDelay = 500.milliseconds
  }

  trait createItem
    extends ItemIndexCleaner
    with FieldValuesIniter
    with After {

    cleanIndex()
    removeData()
    initFieldValues()

    lazy val orgId = OrganizationHelper.create("test-org")
    lazy val collectionId = CollectionHelper.create(orgId)
    lazy val query = ItemIndexQuery(widgets = Seq("corespring-calculator"))

    def searchResult = {
      def trySearch(n: Int): Validation[Error, ItemIndexSearchResult] = {
        Await.result(bootstrap.Main.itemIndexService.search(query), 1.second) match {
          case Success(searchResult) if (searchResult.total > 0) => Success(searchResult)
          case Success(searchResult) if (n == 0) => Success(searchResult)
          case Failure(error) => Failure(error)
          case _ => {
            Thread.sleep(Config.RetryDelay.toMillis)
            trySearch(n - 1)
          }
        }
      }
      trySearch(Config.RetryCount)
    }

    override def after = {
      logger.debug(" ----------- >> after.. cleaning up..")
      removeData()
      cleanIndex()
    }
  }

  val jsonString =
    """
      |{
      |   "contentType" : "item",
      |    "_id": {
      |        "_id": {
      |            "$oid": "521f5c463004534c766ce45b"
      |        },
      |        "version": 1
      |    },
      |    "contentType": "item",
      |    "collectionId": "51df104fe4b073dbbb1c84fa",
      |    "playerDefinition": {
      |        "files": [],
      |        "xhtml": "<div><corespring-calculator id=\"automatically-inserted-calculator\"></corespring-calculator></div>",
      |        "components": {
      |            "automatically-inserted-calculator": {
      |                "componentType": "corespring-calculator"
      |            }
      |        },
      |        "summaryFeedback": ""
      |    },
      |    "taskInfo": {
      |        "title": "this is a test item."
      |    }
      |} """.stripMargin

  trait loadJson extends createItem {
    val dbo = com.mongodb.util.JSON.parse(jsonString).asInstanceOf[DBObject]
    logger.info(s"dbo: $dbo")
    implicit val nc: Context = bootstrap.Main.context
    val dboItem = com.novus.salat.grater[Item].asObject(dbo)
    val item = dboItem.copy(collectionId = collectionId.toString)
  }

  "search" should {
    "find an item with an automatically inserted calculator when it's indexed via the app" in new loadJson {
      implicit val itemFormat = bootstrap.Main.jsonFormatting.item
      logger.debug(s"loaded item json: ${Json.prettyPrint(Json.toJson(item))}")
      //This will add the item via the indexing dao
      ItemHelper.create(collectionId, item)
      //Now search..
      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }

    "find an item with an automatically inserted calculator when index is run manually" in new loadJson {
      //Add the raw dbo to the db
      bootstrap.Main.db(CollectionNames.item).insert(dbo)
      val cfg = bootstrap.Main.elasticSearchConfig
      //Run the indexer
      logger.info(s"config: $cfg")
      val result = ContentIndexer.reindex(cfg.url, cfg.mongoUri, cfg.componentPath)(ExecutionContext.Implicits.global)
      logger.info(s"result? $result")
      //Now search
      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }
  }
}
