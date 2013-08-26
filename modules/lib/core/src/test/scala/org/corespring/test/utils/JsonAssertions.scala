package org.corespring.test.utils

import org.specs2.mutable.Specification
import org.specs2.execute.Result

trait JsonAssertions extends Specification{

  def assertJsonIsEqual(a:String,b:String) : Result = {

    JsonCompare.caseInsensitiveSubTree(a, b) match {
      case Left(diffs) => {
        println(diffs)
        failure(diffs.mkString(","))
      }
      case Right(_) => success
    }
  }
}
