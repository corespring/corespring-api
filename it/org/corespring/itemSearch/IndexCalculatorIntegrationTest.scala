package org.corespring.itemSearch

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.elasticsearch.BatchContentIndexer
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
    lazy val searchResult = Await.result(main.itemIndexService.unboundedSearch(query), 1.second)

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
    implicit val nc: Context = main.context
    val dboItem = com.novus.salat.grater[Item].asObject(dbo)
    val item = dboItem.copy(collectionId = collectionId.toString)
  }

  "search" should {
    "find an item with an automatically inserted calculator when it's indexed via the app" in new loadJson {
      implicit val itemFormat = main.jsonFormatting.item
      logger.debug(s"loaded item json: ${Json.prettyPrint(Json.toJson(item))}")
      //This will add the item via the indexing dao
      ItemHelper.create(collectionId, item)
      //Now search..
      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }

    "find an item with an automatically inserted calculator when index is run manually" in new loadJson {
      //Add the raw dbo to the db
      main.db(CollectionNames.item).insert(dbo)
      val cfg = main.elasticSearchConfig
      //Run the indexer
      logger.info(s"config: $cfg")
      val result = BatchContentIndexer.reindex(cfg.url, cfg.mongoUri, cfg.componentPath)(ExecutionContext.Implicits.global)
      logger.info(s"result? $result")
      /**
       * Note: We have to give the indexer a little bit more time before we search.
       * It should be a blocking call but it appears not.
       */
      Thread.sleep(5000)
      //Now search

      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }
  }
}
