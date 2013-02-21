package tests.api.v1

import org.specs2.mutable.Specification
import utils.RequestCalling
import models.quiz.basic.Quiz
import play.api.mvc.{AnyContentAsJson, AnyContentAsEmpty}
import tests.PlaySingleton
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.mvc.Call
import play.api.test.Helpers._
import play.api.test.FakeRequest

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

    "create" in {
      val q = Quiz(orgId = Some(orgId), metadata = Map("course" -> "some course"))
      val json = AnyContentAsJson(Json.toJson(q))
      val createdQuiz = invokeCall[Quiz](Routes.create(), json)
      createdQuiz.metadata.get("course") === Some("some course")
    }

    "update" in {
      val q = Quiz(orgId = Some(orgId), metadata = Map("course" -> "some course"))
      Quiz.create(q)
      val update = q.copy( metadata = Map("course" -> "some updated course"))
      val json = AnyContentAsJson(Json.toJson(update))
      val updatedQuiz = invokeCall[Quiz](Routes.update(q.id), json)
      updatedQuiz.metadata.get("course") === Some("some updated course")
    }

    "delete" in {
      val q = Quiz(orgId = Some(orgId), metadata = Map("course" -> "some course"))
      Quiz.create(q)

      val call : Call = Routes.delete(q.id)
      routeAndCall(FakeRequest(call.method, call.url, FakeAuthHeader, AnyContentAsEmpty)) match {
        case Some(result) => {
          status(result) === OK
          Quiz.findOneById(q.id) === None
        }
        case _ => failure("Error deleting")
      }
    }
  }

}
