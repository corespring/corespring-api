package org.corespring.itemSearch

import org.corespring.it.helpers.{ OrganizationHelper, CollectionHelper, ItemHelper, SecureSocialHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.it.{ FieldValuesIniter, IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.specs2.mutable.After
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scalaz.Success

class IndexCalculatorIntegrationTest extends IntegrationSpecification {

  trait createItem
    extends ItemIndexCleaner
    with FieldValuesIniter
    with After {

    initFieldValues

    cleanIndex()

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

  trait loadJson extends createItem {
    def path: String
    lazy val url = this.getClass.getResource(path)
    lazy val s = Source.fromFile(url.toURI).getLines().mkString("\n")
    lazy val json = Json.parse(s)
    logger.trace(s"json=${Json.prettyPrint(json)}")
    implicit val itemFormat = bootstrap.Main.jsonFormatting.item
    val item = json.as[Item]
    addItem(item)
    searchResult.map(_.total) must_== Success(1)
  }

  "search" should {
    "find an item that has an automatically inserted calculator in its model" in new loadJson {
      override def path: String = "/indexing/calculator-item-auto-with-qti.json"
      addItem(item)
      searchResult.map(_.total) must_== Success(1)
    }

    "find an item that has an automatically inserted calculator in its model (no qti)" in new loadJson {
      override def path: String = "/indexing/calculator-item-auto-no-qti.json"
      addItem(item)
      searchResult.map(_.total) must_== Success(1)
    }

    "find an item that has a calculator that was added by the itemService" in new createItem {
      val item = itemWithWidget("corespring-calculator")
      addItem(item)
      searchResult.map(_.total) must_== Success(1)
    }
  }
}
