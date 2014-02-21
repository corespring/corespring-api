package org.corespring.platform.core.models.item.json

import org.corespring.platform.core.models.item.Content
import org.corespring.platform.core.models.search.SearchFields
import play.api.libs.json.Writes

case class ContentView[ContentType <: Content[_]](content: ContentType, searchFields: Option[SearchFields])
                                                 (implicit writes: Writes[ContentView[ContentType]])
