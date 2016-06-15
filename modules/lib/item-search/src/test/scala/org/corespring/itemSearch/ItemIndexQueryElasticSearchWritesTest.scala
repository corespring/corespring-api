package org.corespring.itemSearch

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.libs.json.Json._

class ItemIndexQueryElasticSearchWritesTest extends Specification {

  def mkJson(q: ItemIndexQuery): JsValue = toJson(q)(ItemIndexQuery.ElasticSearchWrites)

  "writes" should {

    "default query (mode: latest)" should {

      "set term to latest:true" in {
        val query = ItemIndexQuery()
        val json = mkJson(query)
        (json \ "query").toString must_== """{"bool":{"must":[{"term":{"latest":true}}]}}"""
      }

      "set latest:true + published:true" in {
        val query = ItemIndexQuery(published = Some(true))
        val json = mkJson(query)
        (json \ "query" \ "bool" \ "must").toString must_== """[{"term":{"latest":true}},{"term":{"published":true}}]"""
      }

      "set latest:true + published:false" in {
        val query = ItemIndexQuery(published = Some(false))
        val json = mkJson(query)
        (json \ "query" \ "bool" \ "must").toString must_== """[{"term":{"latest":true}},{"term":{"published":false}}]"""
      }
    }

    "with mode latestPublished" should {

      trait latestPublished extends Scope {
        val query = ItemIndexQuery(mode = SearchMode.latestPublished)
        lazy val json = mkJson(query)
      }

      "set term to latestPublished:true" in new latestPublished {
        (json \ "query" \ "bool" \ "must").toString must_== """[{"term":{"latestPublished":true}}]"""
      }

      "ignore published" in {
        val query = ItemIndexQuery(published = Some(true), mode = SearchMode.latestPublished)
        val json = mkJson(query)
        (json \ "query" \ "bool" \ "must").toString must_== """[{"term":{"latestPublished":true}}]"""
      }
    }
  }

}
