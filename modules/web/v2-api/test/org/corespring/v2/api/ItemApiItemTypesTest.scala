package org.corespring.v2.api

import org.corespring.models.item.ItemType
import org.corespring.platform.core.services.item.ItemIndexService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json._

import scala.concurrent._
import scalaz.Success

class ItemApiItemTypesTest extends Specification with Mockito {

  import ExecutionContext.Implicits.global

  val itemTypes = Map(
    "corespring-multiple-choice" -> "Multiple Choice")

  val mockItemIndexService: ItemIndexService = {
    val service = mock[ItemIndexService]
    service.componentTypes.returns(Future { Success(itemTypes) })
    service
  }

  val itemType = new ItemType(mockItemIndexService)

  "all" should {

    "return item types from ItemIndexService" in {
      itemType.all must be equalTo (JsArray(itemTypes.map { case (value, key) => Json.obj("key" -> key, "value" -> value) }.toSeq))
    }

  }

}
