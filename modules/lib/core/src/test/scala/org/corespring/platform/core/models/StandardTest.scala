package org.corespring.platform.core.models

import org.specs2.mutable.Specification
import org.corespring.test.PlaySingleton

class StandardTest extends Specification {

  PlaySingleton.start()

  "group" should {
//
//    def standard(dotNotation: String): Standard = Standard.findOneByDotNotation(dotNotation) match {
//      case Some(standard) => standard
//      case None => throw new Exception("This standard was not found")
//    }
//
//    "return group without trailing lowercase letter" in {
//      standard("RF.1.3a").group.get === "RF.1.3"
//    }

//    "print a Whitney spreadsheet" in {
//      println("Dot Notation,Subcategory,Grade(s)")
//      Standard.cachedStandards().foreach( standard => {
//        println("\"" + standard.dotNotation.get + "\",\"" + standard.subCategory.get + "\",\"" + standard.grades.mkString(",") + "\"")
//      })
//      true === true
//    }

    "print" in {
      Standard.groupMap.foreach({ case (key, value) => {
        println("\"" + key + "\",\"" + value.map(_.dotNotation.get).mkString("\",\"") + "\"")
      }})
      true === true
    }

  }

}
