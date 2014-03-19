package org.corespring.platform.core.models

import org.specs2.mutable.Specification
import org.corespring.test.PlaySingleton
import scala.util.Sorting

class StandardOrderingTest extends Specification {

  PlaySingleton.start()

  val mathSubject = Some("Math")
  val elaSubject = Some("ELA-Literacy")
  val firstGrade = Seq("01")
  val underGrad = Seq("UG")
  val firstCategory = Some("AAA - This should be first")
  val lastCategory = Some("ZZZ - Because this is a lexographic sort")
  val firstDotNotation = Some("Test.1")
  val secondDotNotation = Some("Test.1a")

  "compare" should {

    "order by subject" in {
      val math = Standard(subject = mathSubject)
      val ela = Standard(subject = elaSubject)
      val standards = Array(math, ela)
      Sorting.quickSort(standards)(StandardOrdering)
      standards.head must be equalTo ela
      standards.last must be equalTo math
    }

    "order by grades when subjects are equal" in {
      val firstGradeStandard = Standard(subject = mathSubject, grades = firstGrade)
      val underGradStandard = Standard(subject = mathSubject, grades = underGrad)
      val standards = Array(underGradStandard, firstGradeStandard)
      Sorting.quickSort(standards)(StandardOrdering)
      standards.head must be equalTo firstGradeStandard
      standards.last must be equalTo underGradStandard
    }

    "order by category when subject and grade are equal" in {
      val first = Standard(subject = mathSubject, grades = firstGrade, category = firstCategory)
      val last = Standard(subject = mathSubject, grades = firstGrade, category = lastCategory)
      val standards = Array(last, first)
      Sorting.quickSort(standards)(StandardOrdering)
      standards.head must be equalTo first
      standards.last must be equalTo last
    }

    "order by code when subject, grade, and category are equal" in {
      val first = Standard(dotNotation = firstDotNotation, subject = mathSubject, grades = firstGrade, category = firstCategory)
      val last = Standard(dotNotation = secondDotNotation, subject = mathSubject, grades = firstGrade, category = firstCategory)
      val standards = Array(last, first)
      Sorting.quickSort(standards)(StandardOrdering)
      standards.head must be equalTo first
      standards.last must be equalTo last
    }

  }

}
