package tests.controllers

import org.specs2.mutable.Specification
import controllers.Utils.shuffle

class UtilsTest extends Specification {
  val elems = List("apple","pear","moon","shine","bell","head")

  def isFixed(e:String) = e.contains("a")
  val iterations = 100

  "shuffle" should {

    val results = collection.mutable.Map[String, Int]()
    elems.foreach(results(_) = 0)
    (1 to iterations) foreach { i=>
      val shuffled = shuffle(elems, isFixed)
      shuffled.zipWithIndex foreach { case(el, i) => results(el) = results(el) + i }
    }

    "not touch fixed elements" in {
      results("apple") mustEqual 0
      results("pear") mustEqual iterations
      results("head") mustEqual 5 * iterations
    }

    "distribute evenly" in {
      println(results)
      results("moon") must beCloseTo(300,30)
      results("shine") must beCloseTo(300,30)
      results("bell") must beCloseTo(300,30)
    }
  }

}
