package org.corespring.platform.core.models.item.json

import org.corespring.models.item.Item.ItemFormat
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.json.ItemView
import org.corespring.models.search.SearchFields
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, Json }

class ItemViewTest extends Specification {

  PlaySingleton.start()

  "ItemView" should {

    "when calling Writes.writes" should {

      case class writeScope(sf: Option[SearchFields] = None) extends Scope {
        val item = Item(taskInfo = Some(TaskInfo(title = Some("Hi"))))
        val rawJson: JsObject = ItemFormat.writes(item).asInstanceOf[JsObject] - "format"
        import ItemView._
        val itemView = ContentView[Item](item, searchFields = sf)
        val result = ItemView.Writes.writes(itemView).as[JsObject] - "format"
      }

      "will not strip anything if searchFields is None" in new writeScope {
        result === rawJson
      }

      def only(include: Boolean, keys: String*): Option[SearchFields] = Some(SearchFields(method = if (include) 1 else 0, jsfields = keys))

      "will only include fields specified in js fields" in new writeScope(only(include = true, "title")) {
        result === Json.obj("title" -> "Hi")
      }

      "will exclude fields specified in js fields" in new writeScope(only(include = false, "title")) {
        result === rawJson - "title"
      }

    }
  }

}
