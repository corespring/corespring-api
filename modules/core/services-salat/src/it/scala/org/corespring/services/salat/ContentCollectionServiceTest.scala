package org.corespring.services.salat

import org.specs2.mutable.Specification

class ContentCollectionServiceTest extends Specification {

  def calling(n:String) = s"when calling $n"

  "ContentCollectionService" should {

    calling("insertCollection") should {
      "work" in pending
    }

    calling("shareItems") should {
      "work" in pending
    }

    calling("getDefaultCollection") should {
      "work" in pending
    }

    calling("unShareItems") should {
      "work" in pending
    }

    calling("getContentCollRefs") should {
      "work" in pending
    }

    calling("getCollectionIds") should {
      "work" in pending
    }

    calling("addOrganizations") should {
      "work" in pending
    }

    calling("isPublic") should {
      "work" in pending
    }

    calling("isAuthorized") should {
      "work" in pending
    }

    calling("delete") should {
      "work" in pending
    }

    calling("getPublicCollections") should {
      "work" in pending
    }

    calling("shareItemsMatchingQuery") should {
      "work" in pending
    }

    calling("itemCount") should {
      "work" in pending
    }
  }
}
