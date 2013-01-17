package models.json

import models.Item
import models.search.SearchFields
import play.api.libs.json.{JsNull, JsValue, Writes}

case class ItemView(item:Item, searchFields:Option[SearchFields])
object ItemView{
  implicit object ItemWrites extends Writes[ItemView]{
    def writes(itemView:ItemView) = {
      JsNull
    }
  }
}
