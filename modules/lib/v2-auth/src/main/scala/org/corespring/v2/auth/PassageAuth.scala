package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{Content, Passage}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.{OrgCollectionService, PassageService}
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scalaz.{Success, Failure, Validation}

trait PassageAuth {
  def loadForRead(passageId: String, itemId: Option[VersionedId[ObjectId]])(implicit identity: OrgAndOpts): Validation[V2Error, Passage]
  def insert(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
  def save(passage: Passage)(implicit identity: OrgAndOpts, executionContext: ExecutionContext): Future[Validation[V2Error, Passage]]
}

class PassageAuthWired(
    passageService: PassageService,
    access: PassageAccess) extends PassageAuth {

  private val DB_TIMEOUT = 20.seconds

  override def loadForRead(id: String, itemId: Option[VersionedId[ObjectId]] = None)
                          (implicit identity: OrgAndOpts): Validation[V2Error, Passage] = VersionedId(id) match {
    case Some(id) => Await.result(passageService.get(id), DB_TIMEOUT) match {
      case Success(maybePassage) => {
        maybePassage match {
          case Some(passage) => access.grant(identity, Permission.Read, (passage, itemId)) match {
            case Success(true) => Success(passage)
            case Success(false) => Failure(inaccessiblePassage(id, identity.org.id, Permission.Read))
            case Failure(error) => Failure(error)
          }
          case None => Failure(cantFindPassageWithId(id))
        }
      }
      case Failure(error) => Failure(couldNotReadPassage(id))
    }
    case _ => Failure(invalidObjectId(id, ""))
  }

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

  def ifWritable(passage: Passage, block: Passage => Future[Validation[V2Error, Passage]])
                 (implicit identity: OrgAndOpts) =
    access.grant(identity, Permission.Write, (passage, None)) match {
      case Success(true) => block(passage)
      case Success(false) => Future.successful(Failure(couldNotWritePassage(passage.id)))
      case Failure(error) => Future.successful(Failure(error))
    }

}