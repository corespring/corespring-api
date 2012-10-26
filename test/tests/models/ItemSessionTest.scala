package tests.models

import org.specs2.mutable.Specification
import models.{ItemSessionSettings, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import tests.PlaySingleton
import models.ItemSession._
import play.api.libs.json.Json.stringify
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json, JsValue}

class ItemSessionTest  extends Specification {

  PlaySingleton.start()


  val DummyXml  = scala.xml.XML.loadFile("test/mockXml/item-session-test-one.xml")

  "json parsing" should {
    "work" in {
      val session = ItemSession( itemId = new ObjectId(), settings = Some(ItemSessionSettings( maxNoOfAttempts = 10)) )
      val json = Json.toJson(session)
      val settings : JsValue = (json\ItemSession.settings)
      (settings \ "maxNoOfAttempts").as[Int] must equalTo(10)
      val newSession : ItemSession = json.as[ItemSession]
      newSession.settings.get.maxNoOfAttempts must equalTo(10)
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

    "update item settings"  in {
      val session = ItemSession( itemId = new ObjectId() )
      ItemSession.save(session)

      session.settings = Some(ItemSessionSettings( submitCompleteMessage = "custom"))

      ItemSession.updateItemSession(session, DummyXml ) match {
        case Left(_) => failure
        case Right(s) => {
          if ( s.settings.get.submitCompleteMessage == "custom") success else failure
        }
      }
    }

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

  "new item session" should {
    "return an unstarted item session" in {
      val session = ItemSession( itemId = new ObjectId() )
      ItemSession.newItemSession( new ObjectId(), session )
      if( session.start.isDefined ) failure else success
    }
  }

  "start item session" should {
    "start the session" in {
      val session = ItemSession( itemId = new ObjectId() )
      ItemSession.save(session)
      if ( session.start.isDefined ) failure
      ItemSession.startItemSession(session) match {
        case Left(_) => failure
        case Right(s) =>  if (s.start.isDefined) success else failure
      }
    }
  }

}
