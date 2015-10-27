package org.corespring.services.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Assessment, Answer }

trait AssessmentService {

  def addAnswer(assessmentId: ObjectId, externalUid: String, answer: Answer): Option[Assessment]
  def addParticipants(assessmentId: ObjectId, externalUids: Seq[String]): Option[Assessment]
  def create(q: Assessment): Unit
  def findAllByOrgId(id: ObjectId): List[Assessment]
  def findByAuthor(authorId: String): List[Assessment]
  def findByAuthorAndOrg(authorId: String, organizationId: ObjectId): List[Assessment]
  def findByIdAndOrg(id: ObjectId, organizationId: ObjectId): Option[Assessment]
  def findByIds(ids: List[ObjectId]): List[Assessment]
  def findByIds(ids: List[ObjectId], organizationId: ObjectId): List[Assessment]
  def findOneById(id: ObjectId): Option[Assessment]
  def remove(q: Assessment): Unit
  def update(q: Assessment): Unit
}
