package org.corespring.services.item

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.item.Content
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait BaseFindAndSaveService[ContentType <: Content[ID], ID] {

  def save(i: ContentType, createNewVersion: Boolean = false): Validation[PlatformServiceError, ID]

  def findOneById(id: ID): Option[ContentType]
}

trait BaseContentService[ContentType <: Content[ID], ID] extends BaseFindAndSaveService[ContentType, ID] {

  def clone(content: ContentType): Option[ContentType]

  def insert(i: ContentType): Option[ID]

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Boolean

}
