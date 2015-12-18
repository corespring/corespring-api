package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.PassageService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{inaccessiblePassage, cantFindPassageWithId, invalidObjectId}
import org.corespring.v2.errors.V2Error

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.{Success, Failure, Validation}

trait PassageAuth[A] extends Auth[Passage, A, VersionedId[ObjectId]] {
  def canCreateInCollection(collectionId: String)(identity: A): Validation[V2Error, Boolean]
  def canWrite(id: String)(implicit identity: A): Validation[V2Error, Boolean]
  def delete(id: String)(implicit identity: A): Validation[V2Error, VersionedId[ObjectId]]
}

class PassageAuthWired(passageService: PassageService, access: PassageAccess) extends PassageAuth[OrgAndOpts] {

  private val DB_TIMEOUT = 20.seconds

  override def loadForRead(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, Passage] = VersionedId(id) match {
    case Some(id) => Await.result(passageService.get(id), DB_TIMEOUT) match { // TODO: Async
      case Some(passage) => access.grant(identity, Permission.Read, passage) match {
        case Success(true) => Success(passage)
        case Success(false) => Failure(inaccessiblePassage(id, identity.org.id, Permission.Read))
        case Failure(error) => Failure(error)
      }
      case None => Failure(cantFindPassageWithId(id))
    }
    case _ => Failure(invalidObjectId(id, ""))
  }


  /** We don't need these yet. **/
  override def canCreateInCollection(collectionId: String)(identity: OrgAndOpts): Validation[V2Error, Boolean] = ???
  override def canWrite(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = ???
  override def delete(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, VersionedId[ObjectId]] = ???
  override def loadForWrite(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, Passage] = ???
  override def insert(data: Passage)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = ???
  override def save(data: Passage, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = ???

}