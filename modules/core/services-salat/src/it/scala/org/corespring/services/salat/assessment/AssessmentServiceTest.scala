package org.corespring.services.salat.assessment

import org.bson.types.ObjectId
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.models.assessment.{ Answer, Participant, Question, Assessment }
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.BeforeAfter

class AssessmentServiceTest extends ServicesSalatIntegrationTest {

  val mostRecently = DateTime.now()

  override def testMostRecentDateModifiedForSessions(ids: Seq[ObjectId]): Option[DateTime] = Some(mostRecently)

  trait scope extends BeforeAfter with InsertionHelper {

    val service = services.assessmentService

    val orgOne = insertOrg("org-1")
    val collectionOne = insertCollection("coll-1", orgOne)
    val itemOne = new Item(
      collectionOne.id.toString,
      standards = Seq("item-standard-1", "item-standard-2"),
      taskInfo = Some(TaskInfo(title = Some("item-title-1"))))

    val questionOne = new Question(itemOne.id, standards = Seq("question-standard-1"))
    val assessmentOne = new Assessment(
      orgId = Some(orgOne.id),
      questions = Seq(questionOne),
      metadata = Map("authorId" -> "Author-1"))
    val sessionOne = ObjectId.get
    val answerOne = Answer(sessionOne, itemOne.id)

    override def before = {
      services.itemService.insert(itemOne)
      service.create(assessmentOne)
    }

    override def after = {
      removeAllData()
    }

    def loadAssessment(id: ObjectId) = service.findOneById(id)
    def loadAssessmentOne() = service.findOneById(assessmentOne.id)

    def participant(uid: String) = Participant(Seq.empty, uid)
  }

  "addAnswer" should {
    trait addAnswerScope extends scope {
      override def before = {
        super.before
        service.addParticipants(assessmentOne.id, Seq("p-1"))
      }
    }
    "add the answer to the participant of the assessment" in new addAnswerScope {
      service.addAnswer(assessmentOne.id, "p-1", answerOne)
      loadAssessmentOne().map(_.participants(0).answers(0)) must_== Some(answerOne)
    }
    "return the updated assessment" in new addAnswerScope {
      service.addAnswer(assessmentOne.id, "p-1", answerOne) must_== loadAssessmentOne()
    }
    "return the unchanged assessment, when the participant cannot be found" in new addAnswerScope {
      val originalAssessment = loadAssessmentOne()
      service.addAnswer(assessmentOne.id, "non-existing-id", answerOne) must_== originalAssessment
    }
    "return the unchanged assessment, when the answer has been added already" in new addAnswerScope {
      service.addAnswer(assessmentOne.id, "p-1", answerOne)
      val answeredAssessment = loadAssessmentOne()
      service.addAnswer(assessmentOne.id, "p-1", answerOne) must_== answeredAssessment
    }
    "return None, if the assessment does not exist" in new addAnswerScope {
      service.addAnswer(ObjectId.get, "p-1", answerOne) must_== None
    }

  }

  "addParticipants" should {
    "add the participant to the assessment" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1"))
      loadAssessmentOne().map(_.participants) must_== Some(Seq(participant("p-1")))
    }
    "add multiple participants to the assessment" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1", "p-2"))
      loadAssessmentOne().map(_.participants) must_== Some(Seq(participant("p-1"), participant("p-2")))
    }
    "return the updated assessment" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1", "p-2")) must_== loadAssessmentOne()
    }
    "return None, when the assessment cannot be found" in new scope {
      service.addParticipants(ObjectId.get, Seq("p-1", "p-2")) must_== None
    }
    //TODO Why do we not check the external uids? Isn't this an error?
    "allow to use the same externalUid multiple times" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1", "p-1"))
      loadAssessmentOne().map(_.participants) must_== Some(Seq(participant("p-1"), participant("p-1")))
    }
  }

  "create" should {
    "create a new assessment" in new scope {
      loadAssessmentOne().map(_.id) must_== Some(assessmentOne.id)
    }
    "update the questions of the assessment with title and standards of item" in new scope {
      val res = loadAssessmentOne().map(a => (a.questions(0).title, a.questions(0).standards))
      res must_== Some((Some("item-title-1"), Seq("item-standard-1", "item-standard-2")))
    }
  }

  "findAllByOrgId" should {
    "return assessment for org" in new scope {
      val insertedAssessment = loadAssessmentOne().get
      service.findAllByOrgId(orgOne.id) must_== Seq(insertedAssessment)
    }
    "return empty list, if org does not have assessments" in new scope {
      val orgTwo = insertOrg("2")
      service.findAllByOrgId(orgTwo.id) must_== Seq.empty
    }
    "return empty list, if org does not exist" in new scope {
      service.findAllByOrgId(ObjectId.get) must_== Seq.empty
    }
  }

  "findByAuthor" should {
    "return assessment for author" in new scope {
      service.findByAuthor("Author-1") must_== Seq(loadAssessmentOne().get)
    }
    "return empty list, if author does not have assessments" in new scope {
      service.findByAuthor("Author-2") must_== Seq.empty
    }
    "set the dateModified of the participant to the result of mostRecentDateModifiedForSessions" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1"))
      val insertedAssessment = loadAssessmentOne().get
      insertedAssessment.participants(0).lastModified must_== None
      service.findByAuthor("Author-1").map(_.participants(0).lastModified) must_== List(Some(mostRecently))
    }
  }

  "findByAuthorAndOrg" should {
    "return assessment for author and org" in new scope {
      service.findByAuthorAndOrg("Author-1", orgOne.id) must_== Seq(loadAssessmentOne().get)
    }
    "return empty list, if author does not have assessments" in new scope {
      service.findByAuthorAndOrg("Author-2", orgOne.id) must_== Seq.empty
    }
    "return empty list, if org does not have assessments" in new scope {
      val orgTwo = insertOrg("2")
      service.findByAuthorAndOrg("Author-1", orgTwo.id) must_== Seq.empty
    }
    "return empty list, if org does not exist" in new scope {
      service.findByAuthorAndOrg("Author-1", ObjectId.get) must_== Seq.empty
    }
    "set the dateModified of the participant to the result of mostRecentDateModifiedForSessions" in new scope {
      service.addParticipants(assessmentOne.id, Seq("p-1"))
      val insertedAssessment = loadAssessmentOne().get
      insertedAssessment.participants(0).lastModified must_== None
      service.findByAuthorAndOrg("Author-1", orgOne.id).map(_.participants(0).lastModified) must_== List(Some(mostRecently))
    }
  }

  "findByIdAndOrg" should {
    "return assessment for id and org" in new scope {
      service.findByIdAndOrg(assessmentOne.id, orgOne.id) must_== loadAssessmentOne()
    }
    "return None when assessment does not exist" in new scope {
      service.findByIdAndOrg(ObjectId.get, orgOne.id) must_== None
    }
    "return None when org does not exist" in new scope {
      service.findByIdAndOrg(assessmentOne.id, ObjectId.get) must_== None
    }
  }

  "findByIds" should {
    "return assessment with id" in new scope {
      service.findByIds(List(assessmentOne.id)) must_== Seq(loadAssessmentOne().get)
    }
    "return empty list when id cannot be found" in new scope {
      service.findByIds(List(ObjectId.get)) must_== Seq.empty
    }
  }

  "findOneById" should {
    "return assessment with id" in new scope {
      service.findOneById(assessmentOne.id) must_== loadAssessmentOne()
    }
    "return None, when id cannot be found" in new scope {
      service.findOneById(ObjectId.get) must_== None
    }
  }

  "remove" should {
    "remove assessment" in new scope {
      service.remove(assessmentOne)
      loadAssessmentOne() must_== None
    }
    "not fail when assessment does not exist" in new scope {
      service.remove(Assessment())
    }
  }

  "update" should {
    "update an existing assessment" in new scope {
      var updatedAssessment = assessmentOne.copy(starts = Some(DateTime.now()))
      service.update(updatedAssessment)
      loadAssessmentOne().map(_.starts) must_== Some(updatedAssessment.starts)
    }
    "update the questions of the assessment with standards data" in new scope {
      var updatedAssessment = assessmentOne.copy(starts = Some(DateTime.now()))
      service.update(updatedAssessment)
      val res = loadAssessmentOne().map(a => (a.questions(0).title, a.questions(0).standards))
      res must_== Some((Some("item-title-1"), Seq("item-standard-1", "item-standard-2")))
    }
  }

}

