package tests.models

import org.specs2.mutable.Specification
import models._
import org.bson.types.ObjectId
import tests.PlaySingleton
import models.ItemSession._
import play.api.libs.json.Json.stringify
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json, JsValue}
import utils.MockXml
import scala.Left
import models.StringItemResponse
import scala.Some
import scala.Right

class ItemSessionTest extends Specification {

  PlaySingleton.start()


  val DummyXml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

  "json parsing" should {
    "work" in {
      val session = ItemSession(itemId = new ObjectId(), settings = ItemSessionSettings(maxNoOfAttempts = 10))
      val json = Json.toJson(session)
      val settings: JsValue = (json \ ItemSession.settings)
      (settings \ "maxNoOfAttempts").as[Int] must equalTo(10)
      val newSession: ItemSession = json.as[ItemSession]
      newSession.settings.maxNoOfAttempts must equalTo(10)
    }
  }


  // create test session bound to random object id
  // in practice ItemSessions need to be bound to an item
  val testSession = ItemSession(new ObjectId())

  "ItemSession" should {

    "be saveable" in {
      ItemSession.save(testSession)
      ItemSession.findOneById(testSession.id) match {
        case Some(result) => success
        case _ => failure
      }
    }

    "be deletable" in {
      ItemSession.remove(testSession)
      ItemSession.findOneById(testSession.id) match {
        case Some(result) => failure
        case _ => success
      }
    }


    "throw an IllegalArgumentException if a feedbackInline node has no identifier" in {
      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)
      session.responses = Seq(StringItemResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-two.xml")

      try {
        ItemSession.process(session, xml)
        failure
      } catch {
        case e: IllegalArgumentException => success
        case _ => failure
      }
    }
  }


  "update item session" should {

    "update item settings" in {
      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)

      session.settings = ItemSessionSettings(submitCompleteMessage = "custom")

      ItemSession.update(session) match {
        case Left(_) => failure
        case Right(s) => {
          if (s.settings.submitCompleteMessage == "custom") success else failure
        }
      }
    }

    "not update settings if session has been started" in {

      val settings = ItemSessionSettings(submitCompleteMessage = "custom")
      val session = ItemSession(itemId = new ObjectId(), settings = settings)
      ItemSession.save(session)
      ItemSession.begin(session)

      session.settings.submitCompleteMessage = "updated custom message"

      ItemSession.update(session) match {
        case Right(is) => {
          is.settings.submitCompleteMessage must equalTo("custom")
          success
        }
        case _ => failure
      }
    }

  }

  "process" should {

    "return feedback contents for an incorrect answer" in {

      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)
      session.responses = Seq(StringItemResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

      ItemSession.process(session, xml) match {
        case Right(newSession) => {
          val json: JsValue = toJson(newSession)
          val feedbackContents = (json \ "sessionData" \ "feedbackContents")
          newSession.sessionData must beSome
          (feedbackContents \ "5").as[String] must equalTo((xml \ "incorrectResponseFeedback").text)
          (feedbackContents \ "6").as[String] must equalTo((xml \ "correctResponseFeedback").text)
        }
        case Left(error) => failure(error.toString)
      }
      true must equalTo(true)
    }

    "automatically finish an item if the max number of attempts has been reached" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 2)
      val session = ItemSession(itemId = new ObjectId(), settings = settings)

      ItemSession.begin(session)

      (1 to settings.maxNoOfAttempts).foreach(c => {

        ItemSession.process(session, DummyXml) match {
          case Left(e) => failure
          case Right(e) => success
        }
      })

      //Should fail now
      ItemSession.process(session, DummyXml) match {
        case Left(e) => success
        case Right(e) => failure
      }
    }

    "automatically start an item if its not started" in {
      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)
      ItemSession.process(session, DummyXml) match {
        case Left(e) => failure("error: " + e.message)
        case Right(processed) => {
          processed.isStarted must equalTo(true)
          success
        }
      }
    }

    "return a finish after the first attempt if only one attempt is allowed" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 1)
      val session = ItemSession(itemId = new ObjectId(), settings = settings)
      ItemSession.begin(session)
      ItemSession.process(session, DummyXml) match {
        case Left(e) => failure
        case Right(s) => s.finish must not beNone
      }
      success
    }

    "return scores for item responses" in {

      val xml = <assessmentItem>
        <responseDeclaration identifier="q1" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>q1Answer</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction identifier="q1"></choiceInteraction>
        </itemBody>
      </assessmentItem>

      val session = ItemSession(itemId = new ObjectId())
      session.responses = Seq(
        StringItemResponse("q1", "q1Answer")
      )

      ItemSession.save(session)
      ItemSession.process(session, xml) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(1)
        }
      }
    }

    "return scores for full qti item" in {

      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)

      session.responses = Seq(
        ArrayItemResponse("rainbowColors", Seq("blue","violet","red")),
        StringItemResponse("winterDiscontent", "york")
      )

      ItemSession.process(session, MockXml.AllItems) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(1)
          s.responses(1).outcome.get.score must equalTo(1)
        }
      }
    }

  }

  "new item session" should {
    "return an unstarted item session" in {
      val session = ItemSession(itemId = new ObjectId())
      ItemSession.newSession(new ObjectId(), session)
      if (session.start.isDefined) failure else success
    }
  }

  "start item session" should {
    "start the session" in {
      val session = ItemSession(itemId = new ObjectId())
      ItemSession.save(session)
      if (session.start.isDefined) failure
      ItemSession.begin(session) match {
        case Left(_) => failure
        case Right(s) => if (s.start.isDefined) success else failure
      }
    }
  }

}
