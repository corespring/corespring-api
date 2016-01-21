package org.corespring.itemSearch

import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class QueryStringParserTest extends Specification {

  import QueryStringParser._

  trait scope extends Scope {

  }

  "scopedSearchQuery" should {

    "return an error if the query is invalid json" in new scope {
      scopedSearchQuery(Some("blah"), Seq.empty).leftMap(_.getMessage) must_== Failure(invalidJson("blah").getMessage)
    }

    "return a query" in new scope {
      scopedSearchQuery(None, Seq.empty) must_== Success(ItemIndexQuery())
    }

    "return a scoped query from 1 collection" in new scope {
      val collectionOne = ObjectId.get
      val collections = Seq(collectionOne)
      val expected = ItemIndexQuery(collections = collections.map(_.toString))
      scopedSearchQuery(None, collections) must_== Success(expected)
    }

    "return a scoped query from 2 collections" in new scope {
      val collectionOne = ObjectId.get
      val collectionTwo = ObjectId.get
      val collections = Seq(collectionOne, collectionTwo)
      val expected = ItemIndexQuery(collections = collections.map(_.toString))
      scopedSearchQuery(None, collections) must_== Success(expected)
    }
  }
}
