package org.corespring.itemSearch
import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.elasticsearch.ContentIndexer
import org.corespring.it.helpers._
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.it.{ FieldValuesIniter, IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.Standard
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.services.salat.bootstrap.CollectionNames
import org.specs2.mutable.After
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Await }
import scala.concurrent.duration._
import scalaz.Success

class IndexCalculatorIntegrationTest extends IntegrationSpecification {

  trait createItem
    extends ItemIndexCleaner
    with FieldValuesIniter
    with After {

    protected def initStandards() = {
      StandardHelper.create(Standard(Some("A.B.C")))
    }

    cleanIndex()
    removeData()
    initFieldValues()
    initStandards()

    lazy val orgId = OrganizationHelper.create("test-org")
    lazy val collectionId = CollectionHelper.create(orgId)
    lazy val query = ItemIndexQuery(widgets = Seq("corespring-calculator"))
    lazy val searchResult = Await.result(bootstrap.Main.itemIndexService.search(query), 1.second)

    def addItem(i: Item): Unit = {
      ItemHelper.create(collectionId, i)
    }

    protected def itemWithWidget(name: String): Item = {

      val pd = PlayerDefinition(
        s"""<div $name="" id="1"></div>""",
        Json.obj("1" ->
          Json.obj("componentType" -> name)))

      Item(collectionId = collectionId.toString,
        playerDefinition = Some(pd))
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
      |  "_id" : {
      |      "_id" : { "$oid": "521f5c463004534c766ce45b" },
      |      "version" : 1
      |  },
      |  "contentType" : "item",
      |  "collectionId" : "51df104fe4b073dbbb1c84fa",
      |  "playerDefinition" : {
      |    "files" : [],
      |    "xhtml" : "<div><corespring-calculator id=\"automatically-inserted-calculator\"></corespring-calculator></div>",
      |    "components" : {
      |      "automatically-inserted-calculator" : {
      |        "componentType" : "corespring-calculator",
      |      }
      |    },
      |    "summaryFeedback" : ""
      |  },
      |  "taskInfo" : {
      |      "title" : "this is a test item."
      |  }
      |}
    """.stripMargin

  trait loadJson extends createItem {
    val dbo = com.mongodb.util.JSON.parse(jsonString).asInstanceOf[DBObject]
    implicit val nc: Context = bootstrap.Main.context
    val dboItem = com.novus.salat.grater[Item].asObject(dbo)
    val item = dboItem.copy(collectionId = collectionId.toString)
  }

  "search" should {
    "find an item that has an automatically inserted calculator in its model" in new loadJson {
      implicit val itemFormat = bootstrap.Main.jsonFormatting.item
      logger.debug(s"loaded item json: ${Json.prettyPrint(Json.toJson(item))}")
      addItem(item)
      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }

    "find one where index is run manually" in new loadJson {
      bootstrap.Main.db(CollectionNames.item).insert(dbo)
      val cfg = bootstrap.Main.elasticSearchConfig
      val result = ContentIndexer.reindex(cfg.url, cfg.mongoUri, cfg.componentPath)(ExecutionContext.Implicits.global)
      logger.info(s"result? $result")
      searchResult.map(_.total) must_== Success(1)
      searchResult.map(_.hits(0).title) must_== Success(Some("this is a test item."))
    }
  }
}
