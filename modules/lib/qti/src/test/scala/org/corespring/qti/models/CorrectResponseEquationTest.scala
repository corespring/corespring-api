package org.corespring.qti.models

import org.specs2.mutable.Specification

class CorrectResponseEquationTest extends Specification{
  val crxml1 = <correctResponse>
                <value>y=(2/5)x+7</value>
               </correctResponse>
  val crxml2 = <correctResponse>
                <value>f=(2/5)g+7</value>
               </correctResponse>
  val crxml3 = <correctResponse>
                 <value>y=2x^2-5x+7</value>
               </correctResponse>
  val crxml4 = <correctResponse>
                 <value>x=t^2-6t+5</value>
               </correctResponse>
  val cr1 = CorrectResponseEquation(crxml1,"line")
  val cr2 = CorrectResponseEquation(crxml2,"line vars:g,f domain:-15->15,5,4 sigfigs:4")
  val cr3 = CorrectResponseEquation(crxml3,"eqn")
  val cr4 = CorrectResponseEquation(crxml4,"eqn: vars:t,x")

  "correct response equation" should {
    "evaluate the same equation as correct" in {
      cr1.isCorrect("y=.4x+7") must beTrue
    }
    "evaluate an equivalent equation as correct" in {
      cr1.isCorrect("y-7=0.4x") must beTrue
    }
    "evaluate an equation with incorrect slope as incorrect" in {
      cr1.isCorrect("y=.5x+7") must beFalse
    }
    "evaluate an equation with incorrect y-intercept as incorrect" in {
      cr1.isCorrect("y=.4x+8") must beFalse
    }
    "evaluate an equivalent equation with a fraction as a slope as correct" in {
      cr1.isCorrect("y=(4/10)x+7") must beTrue
    }
    "be able to parse line attributes and evaluate correct response" in {
      cr2.domain.include must beEqualTo(Seq((-15,15)))
      cr2.domain.notInclude must beEqualTo(Seq(5,4))
      cr2.sigfigs must beEqualTo(4)
      cr2.variables must beEqualTo("g" -> "f")
      cr2.isCorrect("f=(2/5)g+7") must beTrue
    }
    "evaluate the same equation as correct" in {
      cr2.isCorrect("f=.4g+7") must beTrue
    }
    "evaluate an equivalent equation as correct" in {
      cr2.isCorrect("f-7=0.4g") must beTrue
    }
    "evaluate an equation with incorrect slope as incorrect" in {
      cr2.isCorrect("f=.5g+7") must beFalse
    }
    "evaluate an equation with incorrect y-intercept as incorrect" in {
      cr2.isCorrect("f=.4g+8") must beFalse
    }
    "evaluate an equivalent equation with a fraction as a slope as correct" in {
      cr2.isCorrect("f=(4/10)g+7") must beTrue
    }

    //evaluate non-linear equation
    "evaluate a polynomial equation as correct" in {
      cr3.isCorrect("y=2x^2-5x+7") must beTrue
    }
    "evaluate an equivalent equation as correct" in {
      cr3.isCorrect("y-7+5x=2x^2") must beTrue
    }
    "evaluate an incorrect equation as incorrect" in {
      cr3.isCorrect("y=2x^2-4x+7") must beFalse
    }
    "evaluate another incorrect equation as incorrect" in {
      cr3.isCorrect("y=2x^2-4x+8") must beFalse
    }
    "evaluate an equivalent equation with fractions as correct" in {
      cr3.isCorrect("y=(4/2)x^2-(10/2)x+(14/2)") must beTrue
    }
    "evaluate an equivalent equation with weird exponents as correct" in {
      cr3.isCorrect("y=2x^(10/5)-5x^1+7") must beTrue
    }

    //evaluate with variables other than x,y
    "evaluate another polynomial equation as correct" in {
      cr4.isCorrect("x=t^2-6t+5") must beTrue
    }
    "evaluate another equivalent equation as correct" in {
      cr4.isCorrect("x-5=t^2-6t") must beTrue
    }
    "evaluate another incorrect equation as incorrect" in {
      cr4.isCorrect("x=t^2-4t+7") must beFalse
      cr4.isCorrect("x=t^2-5t+8") must beFalse
    }
    "evaluate another equivalent equation with fractions as correct" in {
      cr4.isCorrect("x=t^2-(12/2)t+5") must beTrue
    }
    "evaluate another equivalent equation with weird exponents as correct" in {
      cr4.isCorrect("x=t^(4/2)-6t+5") must beTrue
    }
  }
}
