package org.corespring.itemSearch

import org.corespring.itemSearch.ItemIndexQuery.Defaults
import org.specs2.mutable.Specification
import play.api.libs.json.Json._

class ItemIndexQueryApiReadsTest extends Specification {

  "reads" should {

    "read the defaults" in {
      val query = ItemIndexQuery.ApiReads.reads(obj()).asOpt.get
      query.latest must_== Defaults.latest
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

    "read latest" should {

      "be None for skip" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("latest" -> "skip")).asOpt.get
        query.latest must_== None
      }

      "be Some(false) for no" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("latest" -> "no")).asOpt.get
        query.latest must_== Some(false)
      }

      "be Some(true) for yes" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("latest" -> "yes")).asOpt.get
        query.latest must_== Some(true)
      }

      "be Some(true) for anything else" in {
        val query = ItemIndexQuery.ApiReads.reads(obj("latest" -> "hello-there??")).asOpt.get
        query.latest must_== Some(true)
      }
    }
  }

}
