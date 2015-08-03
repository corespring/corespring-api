package org.corespring.platform.core.models.item.json

import org.corespring.models.User
import org.corespring.models.item.{ Item, Content }
import org.corespring.models.search.SearchFields
import play.api.libs.json.Writes

case class ContentView[ContentType <: Content[_]](content: ContentType, searchFields: Option[SearchFields])(implicit writes: Writes[ContentView[ContentType]])

//case class CmsItemView(item:Item, searchFields: Option[SearchFields], user : Option[User] = None)

// get all drafts for org ==// Seq[Draft]
object CmsItemJson {
  def apply(item: Item, searchFields: Option[SearchFields], user: Option[User]) = {

    //drafts.find(_id -> item.id, _id.version -> item.id.version, user -> user)
  }
}
