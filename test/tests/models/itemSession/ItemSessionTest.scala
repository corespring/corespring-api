package tests.models.itemSession

import models.item.Item
import models.item.resource.{VirtualFile, Resource}
import models.itemSession._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, JsValue}
import scala.Left
import scala.Right
import scala.Some
import tests.BaseTest
import utils.MockXml
import xml.Elem

class ItemSessionTest extends BaseTest {

  import models.itemSession.ItemSession.Keys

  val DummyXml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

  val itemSession = DefaultItemSession

  "json parsing" should {
    "work" in {
      val session = ItemSession(itemId = new ObjectId(), settings = ItemSessionSettings(maxNoOfAttempts = 10))
      val json = Json.toJson(session)
      val settings: JsValue = (json \ Keys.settings)
      (settings \ "maxNoOfAttempts").as[Int] must equalTo(10)
      val newSession: ItemSession = json.as[ItemSession]
      newSession.settings.maxNoOfAttempts must equalTo(10)
    }
  }


  // create test session bound to random object id
  // in practice itemSessions need to be bound to an item
  val testSession = ItemSession(new ObjectId())

  "itemSession" should {

    "be saveable" in {
      itemSession.save(testSession)
      itemSession.findOneById(testSession.id) match {
        case Some(result) => success
        case _ => failure
      }
    }

    "be deletable" in {
      itemSession.remove(testSession)
      itemSession.findOneById(testSession.id) match {
        case Some(result) => failure
        case _ => success
      }
    }


    "throw an IllegalArgumentException if a feedbackInline node has no identifier" in {
      val session = ItemSession(itemId = new ObjectId())
      itemSession.save(session)
      session.responses = Seq(StringItemResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-two.xml")

      try {
        itemSession.process(session, xml)
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
      itemSession.save(session)

      session.settings = ItemSessionSettings(submitCompleteMessage = "custom")

      itemSession.update(session) match {
        case Left(_) => failure
        case Right(s) => {
          if (s.settings.submitCompleteMessage == "custom") success else failure
        }
      }
    }

    "not update settings if session has been started" in {

      val settings = ItemSessionSettings(submitCompleteMessage = "custom")
      val session = ItemSession(itemId = new ObjectId(), settings = settings)
      itemSession.save(session)
      itemSession.begin(session)

      session.settings.submitCompleteMessage = "updated custom message"

      itemSession.update(session) match {
        case Right(is) => {
          is.settings.submitCompleteMessage must equalTo("custom")
          success
        }
        case _ => failure
      }
    }

  }

  "process" should {

    "return feedback contents for an incorrect answer if settings are set and item is finished" in {

      val session = ItemSession(itemId = new ObjectId())
      session.settings.highlightCorrectResponse
      session.settings.showFeedback = true
      itemSession.save(session)
      session.responses = Seq(StringItemResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

      session.finish = Some(new DateTime())

      itemSession.process(session, xml) match {
        case Right(newSession) => {
          val json: JsValue = toJson(newSession)
          println("finish = " + session.isFinished)
          println(Json.stringify(json))
          val feedbackContents = (json \ "sessionData" \ "feedbackContents")
          newSession.sessionData must beSome
          (feedbackContents \ "5").as[String] must equalTo((xml \ "incorrectResponseFeedback").text)
          (feedbackContents \ "6").as[String] must equalTo((xml \ "correctResponseFeedback").text)
        }
        case Left(error) => failure(error.toString)
      }
      true must equalTo(true)
    }


    "automatically finish an item if only one attempt is allowed" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 1)
      val session = ItemSession(itemId = new ObjectId(), settings = settings)

      itemSession.begin(session)
      itemSession.process(session, DummyXml)

      itemSession.findOneById(session.id) match {
        case Some(s) => {
          s.isFinished === true
        }
        case _ => failure
      }
      true === true
    }

    "automatically finish an item if the max number of attempts has been reached" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 2)
      val session = ItemSession(itemId = new ObjectId(), settings = settings)

      itemSession.begin(session)

      (1 to settings.maxNoOfAttempts).foreach(c => {

        itemSession.process(session, DummyXml) match {
          case Left(e) => failure
          case Right(e) => success
        }
      })

      //Should fail now
      itemSession.process(session, DummyXml) match {
        case Left(e) => success
        case Right(e) => failure
      }
    }

    val SimpleXml = <assessmentItem>
      <responseDeclaration identifier="a" cardinality="single">
        <correctResponse>
          <value>a</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <choiceInteraction responseIdentifier="a">
          <simpleChoice identifier="optiona">a</simpleChoice>
        </choiceInteraction>
      </itemBody>
    </assessmentItem>

    "automatically finish a session if all responses are correct" in {
      val session = ItemSession(itemId = new ObjectId())
      itemSession.begin(session)
      session.responses = Seq(StringItemResponse("a", "a"))
      itemSession.process(session, SimpleXml) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => s.isFinished must equalTo(true)
      }
    }

    "don't automatically finish an item if there is any incorrect responses" in {
      val session = ItemSession(itemId = new ObjectId())
      session.settings.maxNoOfAttempts = 0 // no max... multiple attempts allowed
      itemSession.begin(session)
      session.responses = Seq(StringItemResponse("a", "b"))
      itemSession.process(session, SimpleXml) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => s.isFinished must equalTo(false)
      }
    }

    "automatically start an item if its not started" in {
      val session = ItemSession(itemId = new ObjectId())
      itemSession.save(session)
      itemSession.process(session, DummyXml) match {
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
      itemSession.begin(session)
      itemSession.process(session, DummyXml) match {
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
          <choiceInteraction responseIdentifier="q1"></choiceInteraction>
        </itemBody>
      </assessmentItem>

      val session = ItemSession(itemId = new ObjectId())
      session.responses = Seq(
        StringItemResponse("q1", "q1Answer")
      )

      itemSession.save(session)
      itemSession.process(session, xml) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(1)
        }
      }
    }

    "return scores for full qti item" in {

      val session = ItemSession(itemId = new ObjectId())
      itemSession.save(session)

      session.responses = Seq(
        ArrayItemResponse("rainbowColors", Seq("blue", "violet", "red")),
        StringItemResponse("winterDiscontent", "york")
      )

      itemSession.process(session, MockXml.AllItems) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(3)
          s.responses(1).outcome.get.score must equalTo(0.5)
        }
      }
    }

    "add a dateModified value" in {
      val session = ItemSession(
        itemId = new ObjectId(),
        settings = new ItemSessionSettings(maxNoOfAttempts = 0, allowEmptyResponses = true)
      )
      //Allow multiple attempts
      itemSession.save(session)

      itemSession.findOneById(session.id) match {
        case Some(s) => s.dateModified === None
        case _ => failure("can't find new session")
      }

      session.responses = Seq(StringItemResponse("winterDiscontent", "york"))
      itemSession.process(session, MockXml.AllItems) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.dateModified !== None
        }
      }

      session.finish = Some(new DateTime())
      itemSession.process(session, MockXml.AllItems) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.dateModified === session.finish
        }
      }
    }
  }

  "new item session" should {
    "return an unstarted item session" in {
      val session = ItemSession(itemId = new ObjectId())
      itemSession.newSession(new ObjectId(), session)
      if (session.start.isDefined) failure else success
    }
  }

  "start item session" should {
    "start the session" in {
      val session = ItemSession(itemId = new ObjectId())
      itemSession.save(session)
      if (session.start.isDefined) failure
      itemSession.begin(session) match {
        case Left(_) => failure
        case Right(s) => if (s.start.isDefined) success else failure
      }
    }
  }

  "list multiple" should {
    val ids = List("51116c6287eb055332a2f8e4", "51116bc7a14f7b657a083c1d").map(new ObjectId(_))

    "return multiple" in {
      itemSession.findMultiple(ids) match {
        case Seq(one, two) => success
        case _ => failure
      }
    }

    "return mutliple and ignore unknown ids" in {
      itemSession.findMultiple(ids :+ new ObjectId()) match {
        case Seq(one, two) => success
        case _ => failure
      }
    }
  }


  "get total score" should {
    "return the correct score" in {

      val item = new Item(
        id = new ObjectId(),
        data = Some(Resource(
          "data",
          files =
            Seq(
              VirtualFile(
                name = "qti.xml",
                contentType = "text/xml",
                isMain = true,
                content = MockXml.AllItems.mkString("\n"))
            )
        ))
      )

      Item.save(item)

      val session = ItemSession(itemId = item.id)

      session.responses = Seq(
        ArrayItemResponse("rainbowColors", Seq("blue", "violet", "red")),
        StringItemResponse("winterDiscontent", "york")
      )

      val xml: Elem = itemSession.getXmlWithFeedback(session).right.get

      session.finish = Some(new DateTime())
      itemSession.process(session, xml)

      val (score, maxScore) = itemSession.getTotalScore(session)

      score === 2.0
      maxScore === 7.0
    }

    "return correct score from full quiz" in {

      def assertScore(id: String, expectedScore: Double, expectedTotal: Double): org.specs2.execute.Result = {
        itemSession.findOneById(new ObjectId(id)) match {
          case Some(s) => {
            val (score, total) = DefaultItemSession.getTotalScore(s)
            total === expectedTotal
            score === expectedScore
          }
          case _ => failure("can't find item with Id " + id)
        }
      }
      assertScore("515594f4e4b05a52e550d41a", 2.0, 7.0)
      assertScore("5155a5965c2a32164f2d5046", 1.0, 7.0)
      assertScore("5155a8e6ed0db8d2dd136e85", 1.0, 1.0)
    }
  }
}
