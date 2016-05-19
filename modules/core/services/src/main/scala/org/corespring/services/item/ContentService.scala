package org.corespring.services.item

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.item.Content
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future
import scalaz.Validation

trait BaseFindAndSaveService[ContentType <: Content[ID], ID] {

  def save(i: ContentType, createNewVersion: Boolean = false): Validation[PlatformServiceError, ID]

  def findOneById(id: ID): Option[ContentType]
}

trait BaseContentService[ContentType <: Content[ID], ID] extends BaseFindAndSaveService[ContentType, ID] {

  def clone(content: ContentType): Validation[String, ContentType]

  def insert(i: ContentType): Option[ID]

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Validation[PlatformServiceError, Unit]

  type VidPerm = (VersionedId[ObjectId], Permission)

  def isAuthorizedBatch(orgId: ObjectId, idAndPermissions: (VidPerm)*): Future[Seq[(VidPerm, Boolean)]]

}
