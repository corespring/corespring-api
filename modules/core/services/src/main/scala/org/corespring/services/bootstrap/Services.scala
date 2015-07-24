package org.corespring.services.bootstrap

import org.corespring.services.assessment.{ AssessmentTemplateService, AssessmentService }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.item.{ FieldValueService, ItemService }
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.services._

trait Services {
  def contentCollection: ContentCollectionService
  def metadataSet: MetadataSetService
  def item: ItemService
  def org: OrganizationService
  def user: UserService
  def registration: RegistrationService
  def metadata: MetadataService
  def assessment: AssessmentService
  def assessmentTemplate: AssessmentTemplateService
  def apiClient: ApiClientService
  def token: AccessTokenService
  def subject: SubjectService
  def standard: StandardService
  def fieldValue: FieldValueService
}
