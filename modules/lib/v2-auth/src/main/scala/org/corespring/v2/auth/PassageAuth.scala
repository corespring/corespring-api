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

trait PassageAuth {
  def loadForRead(passageId: String, itemId: Option[VersionedId[ObjectId]])(implicit identity: OrgAndOpts): Validation[V2Error, Passage]
}

class PassageAuthWired(passageService: PassageService, access: PassageAccess) extends PassageAuth {

  private val DB_TIMEOUT = 20.seconds

  override def loadForRead(id: String, itemId: Option[VersionedId[ObjectId]] = None)
                          (implicit identity: OrgAndOpts): Validation[V2Error, Passage] = VersionedId(id) match {
    case Some(id) => Await.result(passageService.get(id), DB_TIMEOUT) match { // TODO: Async
      case Some(passage) => access.grant(identity, Permission.Read, (passage, itemId)) match {
        case Success(true) => Success(passage)
        case Success(false) => Failure(inaccessiblePassage(id, identity.org.id, Permission.Read))
        case Failure(error) => Failure(error)
      }
      case None => Failure(cantFindPassageWithId(id))
    }
    case _ => Failure(invalidObjectId(id, ""))
  }

}