package org.corespring.search.indexing

import com.sksamuel.elastic4s.CustomizedTokenFilter
import org.elasticsearch.common.xcontent.XContentBuilder

case class NGramTokenFilter(override val name: String,
                            minGram: Int = 1,
                            maxGram: Int = 2) extends CustomizedTokenFilter(name) {
  override def build(source: XContentBuilder): Unit = {
    source.field("type", "ngram")
    if (minGram > 0) source.field("minGram", minGram)
    if (maxGram < Integer.MAX_VALUE) source.field("maxGram", maxGram)
  }
}
