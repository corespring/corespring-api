package org.corespring.itemSearch

import org.specs2.mutable.Specification

class ItemIndexQueryTest extends Specification {

  "scopeToCollections" should {

    "returns a query with collections set to the scoped collections" in {
      ItemIndexQuery().scopeToCollections("one", "two", "three").collections must_== Seq("one", "two", "three")
    }

    "returns a query with collections scoped to collections passed in as arguments" in {
      ItemIndexQuery(collections = Seq("one", "two", "three")).scopeToCollections("one").collections must_== Seq("one")
    }

    "returns a query with collections scoped to collections passed in as arguments, but doesn't add extra ids passed in" in {
      ItemIndexQuery(collections = Seq("one", "three")).scopeToCollections("one", "two").collections must_== Seq("one")
    }
  }
}
