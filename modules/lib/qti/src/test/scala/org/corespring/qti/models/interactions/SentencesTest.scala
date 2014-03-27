package org.corespring.qti.models.interactions

import org.specs2.mutable.Specification

class SentencesTest extends Specification {

  "split" should {

    "correctly parse sentences with salutations" in {
      val sentences = Seq("My name is Mr. Burton.", "I am a software engineer!")
      val body = sentences.mkString(" ")
      println(Sentences.split(body))
      println(sentences)
      Sentences.split(body).zip(sentences).map{ case(one, two) => {
        one must be equalTo two
      }}
    }

  }

}
