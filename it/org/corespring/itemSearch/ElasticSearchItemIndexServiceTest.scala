package org.corespring.itemSearch

import org.corespring.it.helpers.{ ItemHelper, OrganizationHelper, CollectionHelper }
import org.corespring.it.{ ItemIndexCleaner, IntegrationSpecification }
import org.corespring.models.item.{ PlayerDefinition, Item }
import org.specs2.mutable.After
import org.specs2.specification.Scope
import play.api.libs.json.Json

import scalaz.Success

class ItemIndexServiceIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with ItemIndexCleaner with After {

    val orgId = OrganizationHelper.create("test-org")
    val collectionId = CollectionHelper.create(orgId)

    cleanIndex()
    val service = bootstrap.Main.itemIndexService

    protected def itemWithWidget(name: String): Item = {

      val pd = PlayerDefinition(
        s"""<div $name="" id="1"></div>""",
        Json.obj("1" ->
          Json.obj("componentType" -> name)))

      Item(collectionId = collectionId.toString,
        playerDefinition = Some(pd))
    }

    override def after = {
      //logger.debug(s"cleaning data...")
      //removeData()
      //cleanIndex()
    }
  }

  "distinct" should {

    "return widgets" in new scope {
      val result = service.distinct("taskInfo.widgets")
      result must equalTo(Success(Seq.empty[String])).await
      ItemHelper.create(collectionId, itemWithWidget("corespring-calculator"))
      logger.debug("now check again...")
      val secondResult = service.distinct("taskInfo.widgets")
      secondResult must equalTo(Success(Seq("corespring-calculator"))).await
    }
  }
}
