package tests.api.v1

import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 9/13/12
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
object ItemQueryTest extends Specification {

  val orquery = "{\"$or\":[{\"gradeLevel\":\"04\"},{\"gradeLevel\":\"08\"}]}"
  val andquery = "{\"$and\":[{\"gradeLevel\":\"04\"},{\"gradeLevel\":\"08\"}]}"
  val inquery = "{reviewsPassed:{\"$in\":[\"Bias\",\"Content\"]}}"
  val allquery = "{\"$all\":[\"04\",\"08\"]}"
  val standardsquery = "{\"standards.dotNotation\":\"1.RL.1\""
  val standardsorquery = "&{\"$or\":[{\"standards.subCategory\":\"Key Ideas and Details\"},{\"standards.subCategory\":\"Craft and Structure\"}]}"

  "the results of querying items with " + orquery should {
    "contain items with gradeLevels containing either 04 or 08" in {
      pending
    }
    "not contain items with gradeLevel of only 11" in {
      pending
    }
  }
  "the results of querying items with " + andquery should {
    "contain both items with gradeLevels of both 04 and 08" in {
      pending
    }
    "not contain items with gradeLevel of only 04 or only 08" in {
      pending
    }
    "not contain items with gradeLevel of only 11" in {
      pending
    }
  }
}
