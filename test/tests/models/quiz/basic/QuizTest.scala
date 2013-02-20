package tests.models.quiz.basic

import org.specs2.mutable.{After, Specification}
import tests.PlaySingleton
import models.quiz.basic.Quiz

class QuizTest extends Specification {

  PlaySingleton.start

  "Quiz" should {
    "save" in new cleanDb {
      val q = Quiz()
      Quiz.create(q)
      Quiz.count() === 1
    }
  }
}

class cleanDb extends After {

  def after {
    Quiz.removeAll()
  }
}
