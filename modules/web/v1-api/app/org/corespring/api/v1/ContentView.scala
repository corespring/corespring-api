package org.corespring.api.v1

import org.corespring.models.item.Content
import org.corespring.platform.core.models.search.SearchFields
import play.api.libs.json.Writes

case class ContentView[ContentType <: Content[_]](content: ContentType, searchFields: Option[SearchFields])(implicit writes: Writes[ContentView[ContentType]])
