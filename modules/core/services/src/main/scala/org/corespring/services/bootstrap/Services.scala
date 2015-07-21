package org.corespring.services.bootstrap

import org.corespring.services.item.ItemService
import org.corespring.services.metadata.MetadataSetService
import org.corespring.services.{ UserService, OrganizationService, ContentCollectionService, RegistrationService }

trait Services {
  def contentCollection: ContentCollectionService

  def metadata: MetadataSetService

  def item: ItemService

  def org: OrganizationService

  def user: UserService

  def registration: RegistrationService
}
