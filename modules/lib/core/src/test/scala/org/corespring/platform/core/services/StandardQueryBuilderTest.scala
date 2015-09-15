package org.corespring.platform.core.services

import com.mongodb.DBObject
import org.specs2.matcher.{ Matcher, Expectable }
import org.specs2.mutable.Specification

class StandardQueryBuilderTest extends Specification {

  abstract class convert(expected: String) extends Matcher[String] with StandardQueryBuilder {

    def convert(s: String): Option[DBObject]

    private def suck(s: String) = {
      s.replaceAll("\\s", "").replaceAll("\\r", "").replaceAll("\\t", "")
    }

    def apply[S <: String](s: Expectable[S]) = {
      val dbo = convert(s.value).get
      val serialized = com.mongodb.util.JSON.serialize(dbo)
      val same = suck(serialized) == suck(expected)

      result(same,
        s"${s.value} converts correctly",
        s"${serialized} does not equal: $expected",
        s)
    }
  }

  case class convertSearch(expected: String) extends convert(expected) {
    override def convert(s: String): Option[DBObject] = getStandardBySearchQuery(s)
  }

  case class convertDotNotation(expected: String) extends convert(expected) {
    override def convert(s: String): Option[DBObject] = getStandardByDotNotationQuery(s)
  }

  "getStandardBySearchQuery" should {

    "return filters in the dbo" in {
      """{"filters" : {"subject" : "ELA", "foo" : "bar"}}""" must convertSearch("""{ "subject": "ELA", "foo":"bar"}""")
    }

    "return searchTerm in the dbo" in {
      """{"searchTerm" : "a"}""" must convertSearch(
        """{ "$or" : [
          { "standard" : { "$regex" : "a" , "$options" : "i"}} ,
          { "subject" : { "$regex" : "a" , "$options" : "i"}} ,
          { "category" : { "$regex" : "a" , "$options" : "i"}} ,
          { "subCategory" : { "$regex" : "a" , "$options" : "i"}} ,
          { "dotNotation" : { "$regex" : "a" , "$options" : "i"}}
          ]}""")
    }

    "return searchTerm + filters in the dbo" in {
      """{"filters" : {"foo": "bar"}, "searchTerm" : "a"}""" must convertSearch(
        """{ "foo" : "bar",
           "$or" : [
          { "standard" : { "$regex" : "a" , "$options" : "i"}} ,
          { "subject" : { "$regex" : "a" , "$options" : "i"}} ,
          { "category" : { "$regex" : "a" , "$options" : "i"}} ,
          { "subCategory" : { "$regex" : "a" , "$options" : "i"}} ,
          { "dotNotation" : { "$regex" : "a" , "$options" : "i"}}
          ]}""")
    }

  }

  "getStandardByDotNotation" should {

    "return dotnotation in the dbo" in {
      """{"dotNotation": "A.1"}""" must convertDotNotation("""{"dotNotation" : "A.1"}""")
    }
  }
}
