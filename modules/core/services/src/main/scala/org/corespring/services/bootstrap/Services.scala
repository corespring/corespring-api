package org.corespring.services.bootstrap

import org.corespring.services.assessment.{ AssessmentTemplateService, AssessmentService }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.item.{ ItemAggregationService, FieldValueService, ItemService }
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.services._

trait Services {
  def cloneItemService: CloneItemService
  def metadataSetService: MetadataSetService
  def itemService: ItemService
  def itemAggregationService: ItemAggregationService
  def contentCollectionService: ContentCollectionService
  def orgCollectionService: OrgCollectionService
  def shareItemWithCollectionsService: ShareItemWithCollectionsService
  def orgService: OrganizationService
  def userService: UserService
  def registrationTokenService: RegistrationTokenService
  def metadataService: MetadataService
  def assessmentService: AssessmentService
  def assessmentTemplateService: AssessmentTemplateService
  def apiClientService: ApiClientService
  def tokenService: AccessTokenService
  def subjectService: SubjectService
  def standardService: StandardService
  def fieldValueService: FieldValueService
}
