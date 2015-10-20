package org.corespring.models

import org.specs2.mutable.Specification

class DomainTest extends Specification {

  val one = Standard(category = Some("cat-one"), dotNotation = Some("C.1"))
  val two = Standard(category = Some("cat-one"), dotNotation = Some("C.1.2"))
  val three = Standard(category = Some("cat-two"), dotNotation = Some("C.2"))

  "apply" should {

    "build a domain" in {
      Domain.fromStandards(Seq(one, two, three), s => s.category) === Seq(
        Domain("cat-one", Seq("C.1", "C.1.2")),
        Domain("cat-two", Seq("C.2")))
    }

    "move empty categories to the 'no-category' domain" in {

      val empty = Standard(category = Some(""), dotNotation = Some("EM.1"))
      Domain.fromStandards(Seq(one, two, empty), s => s.category) === Seq(
        Domain("cat-one", Seq("C.1", "C.1.2")),
        Domain("no-category", Seq("EM.1")))

    }
  }
}
