package tests.models

import org.specs2.mutable.Specification
import models.{ItemResponse, ItemSession}
import org.bson.types.ObjectId
import tests.PlaySingleton
import models.ItemSession._
import play.api.libs.json.Json.stringify
import play.api.libs.json.Json.toJson
import play.api.libs.json.JsValue

class ItemSessionTest  extends Specification {

  PlaySingleton.start()

  "item session" should {
    "throw an IllegalArgumentException if a feedbackInline node has no identifier" in {
      val session = ItemSession( itemId = new ObjectId() )
      ItemSession.save(session)
      session.responses = Seq( ItemResponse( id = "RESPONSE", value="ChoiceB"))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-two.xml")

      try {
        ItemSession.updateItemSession( session , xml )
        failure
      } catch {
        case e : IllegalArgumentException => success
        case _ => failure
      }
    }
  }

  "update item session" should {

    "return feedback contents for an incorrect answer" in {

      val session = ItemSession( itemId = new ObjectId() )
      ItemSession.save(session)
      session.responses = Seq( ItemResponse( id = "RESPONSE", value="ChoiceB"))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

      ItemSession.updateItemSession( session , xml ) match {
        case Right(newSession) => {
          val json : JsValue = toJson(newSession)
          val feedbackContents = (json \ "sessionData" \ "feedbackContents")
          newSession.sessionData must beSome
          (feedbackContents \ "5").as[String] must equalTo( (xml \ "incorrectResponseFeedback").text )
          (feedbackContents \ "6").as[String] must equalTo( (xml \ "correctResponseFeedback").text )
        }
        case Left(error) => failure(error.toString)
      }
      true must equalTo(true)
    }
  }

}
