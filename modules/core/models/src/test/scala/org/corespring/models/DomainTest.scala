package org.corespring.models

import org.specs2.mutable.Specification

class DomainTest extends Specification {

  "apply" should {

    "build a domain" in {

      val one = Standard(category = Some("cat-one"), dotNotation = Some("C.1"))
      val two = Standard(category = Some("cat-one"), dotNotation = Some("C.1.2"))
      val three = Standard(category = Some("cat-two"), dotNotation = Some("C.2"))
      Domain.fromStandards(Seq(one, two, three), s => s.category) === Seq(
        Domain("cat-one", Seq("C.1", "C.1.2")),
        Domain("cat-two", Seq("C.2")))
    }
  }
}
