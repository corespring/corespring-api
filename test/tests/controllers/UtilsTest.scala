package tests.controllers

import org.specs2.mutable.Specification
import controllers.Utils.shuffle
import controllers.Utils.isTrue

class UtilsTest extends Specification {
  val elems = List("apple","pear","moon","shine","bell","head")

  def isFixed(e:String) = e.contains("a")
  val iterations = 100

  "isTrue" should {

    "test boolean" in {
      isTrue(true) must beTrue
      isTrue(false) must beFalse
    }

    "test string" in {
      isTrue("true") must beTrue
      isTrue("TrUe") must beTrue
      isTrue("") must beFalse
      isTrue("false") must beFalse
    }

    "test node" in {
      val trueNode = <node id="true"></node>
      val falseNode = <node id="false"></node>
      isTrue(trueNode.attribute("id").get) must beTrue
      isTrue(falseNode.attribute("id").get) must beFalse
    }

    "test option" in {
      isTrue(Some(true)) must beTrue
      isTrue(Some(false)) must beFalse
      isTrue(None) must beFalse
    }

    "test option node" in {
      val trueNode = <node id="true"></node>
      val falseNode = <node id="false"></node>
      isTrue(trueNode.attribute("id")) must beTrue
      isTrue(falseNode.attribute("id")) must beFalse
    }

  }

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
