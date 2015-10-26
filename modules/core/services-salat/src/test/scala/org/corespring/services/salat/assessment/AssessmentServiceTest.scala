package org.corespring.services.salat.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Assessment, Answer }
import org.specs2.mock.Mockito
import org.specs2.mutable.{ BeforeAfter, Specification }

class AssessmentServiceTest extends Specification with Mockito {

  trait scope extends BeforeAfter {

    override def before = {

    }

    override def after = {

    }
  }

  "addAnswer" should {
    "add the answer to the participant of the assessment" in pending
    "return the updated assessment" in pending
    "return None, if the assessment does not exist" in pending
    "return the unchanged assessment when the participant cannot be found" in pending
    "return the unchanged assessment when the answer has been added already" in pending
  }

  "addParticipants" should {
    "add the participant to the assessment" in pending
    "add multiple participants to the assessment" in pending
    "return the updated assessment" in pending
    "return None, when the assessment cannot be found" in pending
    //TODO Why do we not check the external uids? Isn't this an error?
    "allow to use the same externalUid multiple times" in pending
  }

  "create" should {
    "create a new assessment" in pending
    "return the id of the new assessment" in pending
    "update the questions of the assessment with standards data" in pending
  }

  "findAllByOrgId" should {
    "return assessment for org" in pending
    "return multiple assessments for org" in pending
    "return empty list, if org does not have assessments" in pending
    "return empty list, if org does not exist" in pending
  }

  "findByAuthor" should {
    "return assessment for author" in pending
    "return multiple assessments for author" in pending
    "return empty list, if author does not have assessments" in pending
    "return empty list, if author does not exist" in pending
    "set the dateModified of the participant to the result of mostRecentDateModifiedForSessions"
  }

  "findByAuthorAndOrg" should {
    "return assessment for author and org" in pending
    "return multiple assessments for author and org" in pending
    "return empty list, if author does not have assessments" in pending
    "return empty list, if org does not have assessments" in pending
    "return empty list, if author does not exist" in pending
    "return empty list, if org does not exist" in pending
    "return empty list, if combination of author and org does not exist" in pending
    "set the dateModified of the participant to the result of mostRecentDateModifiedForSessions"
  }

  "findByIdAndOrg" should {
    "return assessment for id and org" in pending
    "return None when id does not exist" in pending
    "return None when org does not exist" in pending
    "return None when combination of id and org does not exist" in pending
  }

  "findByIds" should {
    "return assessment with id" in pending
    "return multiple assessments with id" in pending
    "return empty list when no id can be found" in pending
  }

  "findOneById" should {
    "return assessment with id" in pending
    "return None, when id cannot be found" in pending
  }

  "remove" should {
    "remove assessment" in pending
    "not fail when assessment does not exist" in pending
  }

  "update" should {
    "update an existing assessment" in pending
    "update the questions of the assessment with standards data" in pending
  }

}

