package tests.models.quiz.basic

import org.specs2.mutable.{After, Specification}
import tests.PlaySingleton
import models.quiz.basic.Quiz

class QuizTest extends Specification {

  PlaySingleton.start()

  "Quiz" should {
    "save" in {

      val count = Quiz.count()
      val q = Quiz()
      Quiz.create(q)
      Quiz.count() === (count + 1)
      Quiz.remove(q)
      Quiz.count() === count
    }
  }
}

