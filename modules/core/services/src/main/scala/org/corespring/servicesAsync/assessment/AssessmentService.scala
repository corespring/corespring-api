package org.corespring.servicesAsync.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Assessment, Answer }
import scala.concurrent.Future

trait AssessmentService {

  def addAnswer(assessmentId: ObjectId, externalUid: String, answer: Answer): Future[Option[Assessment]]
  def addParticipants(assessmentId: ObjectId, externalUids: Seq[String]): Future[Option[Assessment]]
  def create(q: Assessment): Future[Unit]
  def findAllByOrgId(id: ObjectId): Future[List[Assessment]]
  def findByIds(ids: List[ObjectId]): Future[List[Assessment]]
  def findByIds(ids: List[ObjectId], organizationId: ObjectId): Future[List[Assessment]]
  def findByAuthor(authorId: String): Future[List[Assessment]]
  def findByAuthorAndOrg(authorId: String, organizationId: ObjectId): Future[List[Assessment]]
  def findOneById(id: ObjectId): Future[Option[Assessment]]
  def findByIdAndOrg(id: ObjectId, organizationId: ObjectId): Future[Option[Assessment]]
  def remove(q: Assessment): Future[Unit]
  def update(q: Assessment): Future[Unit]
}
