package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.PassageService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Success, Failure, Validation}

trait PassageAuth {
  def loadForRead(passageId: String, itemId: Option[VersionedId[ObjectId]])(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
  def insert(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
  def save(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
  def delete(passageId: String)(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
}

class PassageAuthWired(
    passageService: PassageService,
    access: PassageAccess) extends PassageAuth {

  override def loadForRead(passageId: String, itemId: Option[VersionedId[ObjectId]] = None)
                          (implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]] =
    withPassage(passageId, { passage =>
      access.grant(identity, Permission.Read, (passage, itemId)).map(_ match {
        case Success(true) => Success(passage)
        case Success(false) => Failure(inaccessiblePassage(passage.id, identity.org.id, Permission.Read))
        case Failure(error) => Failure(error)
      })
    })

  override def insert(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext) =
    ifWritable(passage, { passage =>
      passageService.insert(passage).map(_ match {
        case Success(passage) => Success(passage)
        case Failure(error) => Failure(couldNotCreatePassage())
      })
    })


  override def save(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext) =
    ifWritable(passage, { passage =>
      passageService.save(passage).map(_ match {
        case Success(passage) => Success(passage)
        case Failure(error) => Failure(couldNotSavePassage(passage.id))
      })
    })

  override def delete(passageId: String)(implicit identity: OrgAndOpts, executionContext: ExecutionContext) =
    withPassage(passageId, { passage =>
      ifWritable(passage, { passage =>
        passageService.delete(passage.id).map(_ match {
          case Success(passage) => Success(passage)
          case Failure(error) => Failure(couldNotDeletePassage(passage.id))
        })
      })
    })

  private def withPassage(passageId: String, block: Passage => Future[Validation[V2Error, Passage]])
                         (implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]] =
    VersionedId(passageId) match {
      case Some(id) => passageService.get(id).flatMap(_ match {
        case Success(maybePassage) => maybePassage match {
          case Some(passage) => block(passage)
          case _ => Future.successful(Failure(cantFindPassageWithId(id)))
        }
        case Failure(error) => Future.successful(Failure(couldNotReadPassage(id)))
      })
      case _ => Future.successful(Failure(invalidObjectId(passageId, "")))
    }

  private def ifWritable(passage: Passage, block: Passage => Future[Validation[V2Error, Passage]])
                 (implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]] =
    access.grant(identity, Permission.Write, (passage, None)).flatMap(_ match {
      case Success(true) => block(passage)
      case Success(false) => Future.successful(Failure(couldNotWritePassage(passage.id)))
      case Failure(error) => Future.successful(Failure(error))
    })

}