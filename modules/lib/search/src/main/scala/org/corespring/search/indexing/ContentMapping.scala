package org.corespring.search.indexing


import com.sksamuel.elastic4s.mapping.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.Strict
import com.sksamuel.elastic4s.{LowercaseTokenFilter, StandardTokenFilter, StandardTokenizer, CustomAnalyzerDefinition}


object ContentMapping {

  def generate = {
    create.index("content").shards(1).mappings(
      "content" as (
        "taskInfo" typed ObjectType as (
          "title" typed StringType searchAnalyzer "ngram_seach_analyzer" indexAnalyzer "ngram_index_analyzer"
          )
        ) dynamic (Strict)
    ) analysis(
      CustomAnalyzerDefinition(
        "ngram_index_analyzer",
        StandardTokenizer,
        StandardTokenFilter,
        LowercaseTokenFilter,
        NGramTokenFilter("ngram", 2, 50)
      ),
      CustomAnalyzerDefinition(
        "ngram_search_analyzer",
        StandardTokenizer,
        StandardTokenFilter,
        LowercaseTokenFilter
      )
      )
  }
}
