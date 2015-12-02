package org.corespring.test

import org.corespring.common.json.JsonCompare
import org.specs2.execute.Result
import org.specs2.mutable.Specification

trait JsonAssertions extends Specification {

  def assertJsonIsEqual(a: String, b: String): Result = {

    JsonCompare.caseInsensitiveSubTree(a, b) match {
      case Left(diffs) => {
        println(diffs)
        failure(diffs.mkString(","))
      }
      case Right(_) => success
    }
  }
}
