package tests.player.controllers

import com.mongodb.DBObject
import com.novus.salat.dao.SalatMongoCursor
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.quiz.basic.{Answer, Quiz}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.player.accessControl.models.RequestedAccess
import org.corespring.player.accessControl.models.RequestedAccess.Mode.Mode
import org.corespring.player.accessControl.models.RequestedAccess.Mode._
import org.corespring.test.PlaySingleton
import org.specs2.execute.{Result => SpecsResult}
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import player.controllers.Views
import scala.xml.Elem
import utils.MockXml

class ViewsTest extends Specification with Mockito {

  import TestIds._

  PlaySingleton.start()

  val mockService = new ItemService{
    def cloneItem(item: Item): Option[Item] = ???
    def findFieldsById(id: VersionedId[ObjectId], fields: DBObject): Option[DBObject] = ???
    def currentVersion(id: VersionedId[ObjectId]): Option[Int] = ???
    def countItems(query: DBObject, fields: Option[String]): Int = ???
    def find(query: DBObject, fields: DBObject): SalatMongoCursor[Item] = ???
    def findOneById(id: VersionedId[ObjectId]): Option[Item] = ???
    def findOne(query: DBObject): Option[Item] = ???
    def save(i: Item, createNewVersion: Boolean) {}
    def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean) {}
    def insert(i: Item): Option[VersionedId[ObjectId]] = ???
    def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[Item] = ???
    def getQtiXml(id: VersionedId[ObjectId]): Option[Elem] = Some( MockXml.AllItems )
    def sessionCount(item: Item): Long = ???
  }

  val quizService = new QuizService{
    def findOneById(id: ObjectId): Option[Quiz] = Some(Quiz(id = id))
    def addAnswer(quizId: ObjectId, externalUid: String, answer: Answer): Option[Quiz] = ???
    def addParticipants(quizId: ObjectId, externalUids: Seq[String]): Option[Quiz] = ???
    def create(q: Quiz) {}
    def findAllByOrgId(id: ObjectId): List[Quiz] = ???
    def findByIds(ids: List[ObjectId]): List[Quiz] = ???
    def remove(q: Quiz) {}
    def update(q: Quiz) {}
  }

  val views = new Views(new TestBuilder, mockService, quizService)

  class BeRightMode(m:Mode) extends Matcher[Action[AnyContent]]{
    def apply[S <: Action[AnyContent]](s:Expectable[S]) = {
      val httpResult = s.value(FakeRequest("", "", FakeHeaders(), AnyContentAsEmpty))
      val modeString = session(httpResult).get(PlayerCookieKeys.ACTIVE_MODE)

      val isOk = status(httpResult) == OK

      result(
        modeString == Some(m.toString) && isOk,
        s"$modeString == status: ${status(httpResult)}, mode: ${m.toString}",
        s"$modeString != status: ${status(httpResult)}, mode: ${m.toString}",
        s)
    }
  }

  def beMode(m:Mode) = new BeRightMode(m)

  "views" should {

    "set the correct cookie" in {
      views.preview(testId) must beMode(Preview)
      views.administerItem(testId) must beMode(Administer)
      views.administerSession(testSessionId) must beMode(Administer)
      views.render(testSessionId) must  beMode(Render)
      views.aggregate(testQuizId, testQuizItemId) must beMode(Aggregate)
      views.profile(testId, "tab") must beMode(Preview)

    }
  }

}
