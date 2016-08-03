package org.corespring.services.salat.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment._
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.mutable.BeforeAfter

class AssessmentTemplateServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter with InsertionHelper {

    val service = services.assessmentTemplateService

    val orgOne = insertOrg("org-1")
    val orgTwo = insertOrg("org-2")
    val collectionOne = insertCollection("coll-1", orgOne)
    val itemOne = Item(
      collectionOne.id.toString,
      standards = Seq("item-standard-1", "item-standard-2"),
      taskInfo = Some(TaskInfo(title = Some("item-title-1"))))

    val questionOne = new Question(itemOne.id, standards = Seq("question-standard-1"))
    val questionTwo = new Question(itemOne.id, standards = Seq("question-standard-2"))
    val templateOne = new AssessmentTemplate(
      orgId = Some(orgOne.id),
      questions = Seq(questionOne),
      metadata = Map("authorId" -> "Author-1"))
    val templateTwo = new AssessmentTemplate(
      orgId = Some(orgTwo.id),
      questions = Seq(questionTwo),
      metadata = Map("authorId" -> "Author-2"))

    override def before = {
      service.insert(templateOne)
    }

    override def after = {
      removeAllData()
    }
  }

  "findByOrg" should {
    "find template by the org" in new scope {
      service.insert(templateTwo)
      val stream = service.findByOrg(orgTwo.id)
      stream.length must_== 1
      stream(0).orgId must_== Some(orgTwo.id)
    }
    "return empty stream if org does not exist" in new scope {
      service.findByOrg(ObjectId.get) must_== Stream.empty
    }
    "return empty stream if contentType is not the default" in new scope {
      val templateWithOtherType = templateTwo.copy(contentType="something other")
      service.insert(templateWithOtherType)
      service.findByOrg(orgTwo.id) must_== Stream.empty
    }
  }

  "findOneById" should {
    //same as dao.findOneById
    "return template by id" in new scope {
      service.findOneById(templateOne.id) must_== Some(templateOne)
    }
  }
  "findOneByIdAndOrg" should {
    "return template by id and org" in new scope {
      service.findOneByIdAndOrg(templateOne.id, orgOne.id) must_== Some(templateOne)
    }
  }
  "insert" should {
    "add the template to db" in new scope {
      service.insert(templateTwo) must_== Some(templateTwo.id)
    }
  }
  "save" should {
    "save the template to db" in new scope {
      service.save(templateOne).isRight must_== true
    }
    "return the id of the saved template" in new scope {
      service.save(templateOne) must_== Right(templateOne.id)
    }
    "update dateModified" in new scope {
      val updatedTemplate = templateOne.copy(dateModified = None)
      service.save(templateOne) must_== Right(templateOne.id)
      service.findOneById(updatedTemplate.id).map(_.dateModified) must_!= None
    }
  }

}

