package org.corespring.itemSearch

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import org.corespring.it.{IntegrationSpecification, ItemIndexCleaner}
import org.corespring.models.item.{Item, PlayerDefinition}
import org.specs2.mutable.After
import play.api.libs.json.Json

class WidgetTypeIntegrationTest extends IntegrationSpecification {

  trait scope
    extends orgWithAccessTokenAndItem
    with ItemIndexCleaner {

    cleanIndex()

    val widgetType = main.widgetType

    override def after = {
      logger.info("after.. cleaning up..")
      removeData()
      cleanIndex()
    }

    protected def itemWithWidget(name: String): Item = {

      val pd = PlayerDefinition(
        s"""<div $name="" id="1"></div>""",
        Json.obj("1" ->
          Json.obj("componentType" -> name)))

      Item(collectionId = collectionId.toString,
        playerDefinition = Some(pd))
    }
  }

  "all" should {

    """return 'corespring-calculator'
      | if an item with a 'corespring-calculator' has been created""".stripMargin in new scope {

      widgetType.all() must_== Json.arr()
      ItemHelper.create(collectionId, itemWithWidget("corespring-calculator"))
      widgetType.all() must_== Json.arr(
        Json.obj("key" -> "corespring-calculator", "value" -> "Calculator"))
    }

    """return 'corespring-calculator'
      | if the item has been created in the collection""".stripMargin in new scope {

      val collections = Seq(collectionId.toString)
      widgetType.all(collections) must_== Json.arr()
      ItemHelper.create(collectionId, itemWithWidget("corespring-calculator"))
      widgetType.all(collections) must_== Json.arr(
        Json.obj("key" -> "corespring-calculator", "value" -> "Calculator"))
    }

    """not return 'corespring-calculator'
      | if the item has been created in another collection""".stripMargin in new scope {

      val collections = Seq(ObjectId.get.toString)
      widgetType.all(collections) must_== Json.arr()
      ItemHelper.create(collectionId, itemWithWidget("corespring-calculator"))
      widgetType.all(collections) must_!= Json.arr(
        Json.obj("key" -> "corespring-calculator", "value" -> "Calculator"))
    }

  }
}
