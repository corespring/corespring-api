package org.corespring.itemSearch

import org.corespring.itemSearch.ItemIndexQuery.Defaults
import org.specs2.mutable.Specification
import play.api.libs.json.Json._

class ItemIndexQueryApiReadsTest extends Specification {

  "reads" should {

    "read the defaults" in {
      val query = ItemIndexQuery.ApiReads.reads(obj()).asOpt.get
      query.mode must_== Defaults.mode
      query.published must_== Defaults.published
      query.text must_== Defaults.text
      query.collections must_== Defaults.collections
      query.contributors must_== Defaults.contributors
      query.count must_== Defaults.count
      query.gradeLevels must_== Defaults.gradeLevels
      query.itemTypes must_== Defaults.itemTypes
      query.metadata must_== Defaults.metadata
      query.offset must_== Defaults.offset
      query.requiredPlayerWidth must_== Defaults.requiredPlayerWidth
      query.sort must_== Defaults.sort
      query.standardClusters must_== Defaults.standardClusters
      query.widgets must_== Defaults.widgets
      query.workflows must_== Defaults.workflows
    }

    "read mode" should {

      "be latest" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("mode" -> "latest")).asOpt.get
        query.mode must_== SearchMode.latest
      }

      "be latestPublished" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("mode" -> "latestPublished")).asOpt.get
        query.mode must_== SearchMode.latestPublished
      }
    }
  }
}
