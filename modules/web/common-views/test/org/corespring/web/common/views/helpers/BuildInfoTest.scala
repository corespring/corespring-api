package org.corespring.web.common.views.helpers

import org.specs2.mutable.Specification

class BuildInfoTest extends Specification {

  "apply" should {

    val props =
      s"""
         |version=1
         |commit.hash=hash
         |date=date
         |branch=branch
         """.stripMargin

    val info = BuildInfo.apply((s) => Some(props))
    "add branch" in {
      info.branch must_== "branch"
    }

    "add pushData" in {
      info.pushDate must_== "date"
    }

    "add commitHashShort" in {
      info.commitHashShort must_== "hash"
    }

    "add version" in {
      info.version must_== "1"
    }
  }
}
