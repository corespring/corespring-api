package org.corespring.services.bootstrap

import org.corespring.services.assessment.{AssessmentTemplateService, AssessmentService}
import org.corespring.services.auth.{AccessTokenService, ApiClientService}
import org.corespring.services.item.{ItemPublishingService, ItemService}
import org.corespring.services.metadata.{MetadataService, MetadataSetService}
import org.corespring.services.{ UserService, OrganizationService, ContentCollectionService, RegistrationService }

trait Services {
  def contentCollection: ContentCollectionService

  def metadataSet: MetadataSetService

  def item: ItemService

  def itemWithPublishing : ItemService with ItemPublishingService

  def org: OrganizationService

  def user: UserService

  def registration: RegistrationService

  def metadata : MetadataService
  def assessment : AssessmentService
  def assessmentTemplate : AssessmentTemplateService

  def apiClient : ApiClientService
  def token:AccessTokenService
}
