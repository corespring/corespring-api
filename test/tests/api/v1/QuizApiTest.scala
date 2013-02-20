package tests.api.v1

import org.specs2.mutable.Specification
import utils.RequestCalling
import models.quiz.basic.Quiz
import play.api.mvc.AnyContentAsEmpty
import tests.PlaySingleton
import org.bson.types.ObjectId

class QuizApiTest extends Specification with RequestCalling {

  PlaySingleton.start()

  val orgId = new ObjectId("51114b307fc1eaa866444648")

  val Routes = api.v1.routes.QuizApi

  "QuizApi" should {
    "get" in {
      val q = Quiz(orgId = Some(orgId))
      Quiz.create(q)
      val requestedQuiz = invokeCall[Quiz](Routes.get(q.id), AnyContentAsEmpty)
      requestedQuiz.id === q.id
    }
  }

}
