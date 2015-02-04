package org.corespring.reporting.utils

import org.specs2.mutable.Specification

class MongoMapReduceUtilsTest extends Specification with MongoMapReduceUtils {

  "fieldCheck" should {

    val objectName = "taskInfo"
    val propertyName = "itemTypes"
    val property = s"$objectName.$propertyName"

    "return JS that performs deep check for field presence" in {
      fieldCheck(property) must beEqualTo(s"this.$objectName && this.$objectName.$propertyName")
    }

  }

}
