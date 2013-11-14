package org.corespring.platform.core.models.itemSession

import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.BaseFile.ContentTypes
import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.responses.{ ArrayResponse, StringResponse }
import org.corespring.test.BaseTest
import org.joda.time.DateTime
import play.api.libs.json.Json.toJson
import play.api.libs.json.{ Json, JsValue }
import scala.Left
import scala.Right
import scala.Some
import utils.MockXml
import xml.Elem

class ItemSessionTest extends BaseTest {

  sequential

  RegisterJodaTimeConversionHelpers()

  import ItemSession.Keys

  val DummyXml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

  val itemSession = DefaultItemSession

  def genItemId = VersionedId(ObjectId.get)

  "json parsing" should {
    "work" in {
      val session = ItemSession(itemId = genItemId, settings = ItemSessionSettings(maxNoOfAttempts = 10))
      val json = Json.toJson(session)
      val settings: JsValue = (json \ Keys.settings)
      (settings \ "maxNoOfAttempts").as[Int] must equalTo(10)
      val newSession: ItemSession = json.as[ItemSession]
      newSession.settings.maxNoOfAttempts must equalTo(10)
    }
  }

  // create test session bound to random object id
  // in practice itemSessions need to be bound to an item
  val testSession = ItemSession(genItemId)

  "itemSession" should {

    "be saveable" in {
      itemSession.save(testSession)
      itemSession.findOneById(testSession.id) match {
        case Some(result) => success
        case _ => failure(s"Couldn't find the newly saved item ${testSession.id}")
      }
    }

    "be deletable" in {
      itemSession.remove(testSession)
      itemSession.findOneById(testSession.id) match {
        case Some(result) => failure(s"The item wasn't deleted: $result")
        case _ => success
      }
    }

    "throw an IllegalArgumentException if a feedbackInline node has no identifier" in {
      val session = ItemSession(itemId = genItemId)
      itemSession.save(session)
      session.responses = Seq(StringResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-two.xml")

      try {
        itemSession.process(session, xml)(false)
        failure
      } catch {
        case e: IllegalArgumentException => success
        case _: Throwable => failure
      }
    }
  }

  "update item session" should {

    "update item settings" in {
      val session = ItemSession(itemId = genItemId)
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
      val session = ItemSession(itemId = genItemId, settings = settings)
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

      val session = ItemSession(itemId = genItemId)
      session.settings.highlightCorrectResponse
      session.settings.showFeedback = true
      itemSession.save(session)
      session.responses = Seq(StringResponse(id = "RESPONSE", responseValue = "ChoiceB", outcome = None))

      val xml = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

      session.finish = Some(new DateTime())

      itemSession.process(session, xml)(false) match {
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
      val session = ItemSession(itemId = genItemId, settings = settings)

      itemSession.begin(session)
      itemSession.process(session, DummyXml)(false)

      itemSession.findOneById(session.id).get.isFinished === true
    }

    "automatically finish an item if the max number of attempts has been reached" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 2)
      val session = ItemSession(itemId = genItemId, settings = settings)

      itemSession.begin(session)

      (1 to settings.maxNoOfAttempts).foreach(c => {

        itemSession.process(session, DummyXml)(false) match {
          case Left(e) => failure
          case Right(e) => success
        }
      })

      //session should be finished now
      itemSession.process(session, DummyXml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => s.isFinished must equalTo(true)
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
      val session = ItemSession(itemId = genItemId)
      itemSession.begin(session)
      session.responses = Seq(StringResponse("a", "a"))
      itemSession.process(session, SimpleXml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => s.isFinished must equalTo(true)
      }
    }

    "don't automatically finish an item if there is any incorrect responses" in {
      val session = ItemSession(itemId = genItemId)
      session.settings.maxNoOfAttempts = 0 // no max... multiple attempts allowed
      itemSession.begin(session)
      session.responses = Seq(StringResponse("a", "b"))
      itemSession.process(session, SimpleXml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => s.isFinished must equalTo(false)
      }
    }

    "automatically start an item if its not started" in {
      val session = ItemSession(itemId = genItemId)
      itemSession.save(session)
      itemSession.process(session, DummyXml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(processed) => {
          processed.isStarted must equalTo(true)
          success
        }
      }
    }

    "return a finish after the first attempt if only one attempt is allowed" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 1)
      val session = ItemSession(itemId = genItemId, settings = settings)
      itemSession.begin(session)
      itemSession.process(session, DummyXml)(false) match {
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

      val session = ItemSession(itemId = genItemId)
      session.responses = Seq(
        StringResponse("q1", "q1Answer"))

      itemSession.save(session)
      itemSession.process(session, xml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(1)
        }
      }
    }

    "return scores for full qti item" in {

      val session = ItemSession(itemId = genItemId)
      itemSession.save(session)

      session.responses = Seq(
        ArrayResponse("rainbowColors", Seq("blue", "violet", "red")),
        StringResponse("winterDiscontent", "york"))

      itemSession.process(session, MockXml.AllItems)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.responses(0).outcome must beSome
          s.responses(0).outcome.get.score must equalTo(3)
          s.responses(1).outcome.get.score must equalTo(0.5)
        }
      }
    }
    "return session outcome with overall score" in{

      val xml = <assessmentItem>
        <responseDeclaration identifier="q1" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>q1Answer</value>
          </correctResponse>
        </responseDeclaration>
        <responseDeclaration identifier="q2" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>q2Answer</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier="q1"></choiceInteraction>
          <choiceInteraction responseIdentifier="q2"></choiceInteraction>
        </itemBody>
      </assessmentItem>

      val session = ItemSession(itemId = genItemId)
      session.responses = Seq(
        StringResponse("q1", "q1Answer"),
        StringResponse("q2", "wrong"))

      itemSession.save(session)
      itemSession.process(session, xml)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.outcome must beSome
          s.outcome.get.score must equalTo(0.5)
        }
      }
    }
    "add a dateModified value" in {
      val session = ItemSession(
        itemId = genItemId,
        settings = new ItemSessionSettings(maxNoOfAttempts = 0, allowEmptyResponses = true))
      //Allow multiple attempts
      itemSession.save(session)

      itemSession.findOneById(session.id) match {
        case Some(s) => s.dateModified === None
        case _ => failure("can't find new session")
      }

      session.responses = Seq(StringResponse("winterDiscontent", "york"))
      itemSession.process(session, MockXml.AllItems)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.dateModified !== None
        }
      }

      session.finish = Some(new DateTime())
      itemSession.process(session, MockXml.AllItems)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          s.dateModified === session.finish
        }
      }
    }

    "if isAttempt is set to false, don't count as a submission" in {
      val session = ItemSession(
        itemId = genItemId,
        settings = new ItemSessionSettings(maxNoOfAttempts = 0, allowEmptyResponses = true))
      //Allow multiple attempts
      itemSession.save(session)
      session.responses = Seq(StringResponse("winterDiscontent", "york"))
      itemSession.process(session, MockXml.AllItems, false)(false) match {
        case Left(e) => failure("error: " + e.message)
        case Right(s) => {
          session.attempts === 0
          session.isFinished === false
        }
      }
      session.responses = Seq(StringResponse("winterDiscontent", "blergl"))
      itemSession.process(session, MockXml.AllItems, false)(false) match {
        case Left(e) => failure("error: "+e.message)
        case Right(s) => {
          println(Json.toJson(s).toString())
          s.responses.exists(r => r.id == "winterDiscontent" && r.value == "blergl") === true
        }
      }
    }
  }

  "new item session" should {
    "return an unstarted item session" in {
      val itemId = genItemId
      itemService.insert(
        Item(
          id = itemId,
          data = Some(
            Resource(name = "data",
              files = Seq(VirtualFile(name = "qti.xml", contentType = ContentTypes.XML, isMain = true, content = "<root/>"))))))
      val session = ItemSession(itemId = itemId)
      itemSession.newSession(session)
      if (session.start.isDefined) failure else success
    }
  }

  "start item session" should {
    "start the session" in {
      val session = ItemSession(itemId = genItemId)
      itemSession.save(session)
      if (session.start.isDefined) failure
      itemSession.begin(session) match {
        case Left(_) => failure
        case Right(s) => if (s.start.isDefined) success else failure
      }
    }
  }

  "get total score" should {
    "return the correct score" in {

      val item = new Item(
        id = genItemId,
        data = Some(Resource(
          "data",
          files =
            Seq(
              VirtualFile(
                name = "qti.xml",
                contentType = "text/xml",
                isMain = true,
                content = MockXml.AllItems.mkString("\n"))))))

      itemService.save(item)

      val session = ItemSession(itemId = item.id)

      session.responses = Seq(
        ArrayResponse("rainbowColors", Seq("blue", "violet", "red")),
        StringResponse("winterDiscontent", "york"))

      val xml: Elem = itemSession.getXmlWithFeedback(session).right.get

      session.finish = Some(new DateTime())
      itemSession.process(session, xml)(false)

      val (score, maxScore) = itemSession.getTotalScore(session)

      score === 0.5
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
      assertScore("515594f4e4b05a52e550d41a", 0.5714285714285714, 7.0)
      assertScore("5155a5965c2a32164f2d5046", 0.14285714285714285, 7.0)
      assertScore("5155a8e6ed0db8d2dd136e85", 1.0, 1.0)
    }
  }

  "reopen" should {
    "reopen a session" in {
      val id = ObjectId.get
      val session = ItemSession(id = id, itemId = genItemId, finish = Some(DateTime.now), attempts = 10)
      val dbId = itemSession.insert(session)
      itemSession.reopen(session)
      itemSession.findOneById(id).map{ s =>
        s.finish.isEmpty === true
        s.attempts === 0
      }.getOrElse(failure(s"Can't find session with id: $id"))
    }
   }
}
