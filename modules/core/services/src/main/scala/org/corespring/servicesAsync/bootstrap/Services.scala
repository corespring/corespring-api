package org.corespring.servicesAsync.bootstrap

import org.corespring.servicesAsync.assessment.{ AssessmentTemplateService, AssessmentService }
import org.corespring.servicesAsync.auth.{ AccessTokenService, ApiClientService }
import org.corespring.servicesAsync.item.{ ItemAggregationService, FieldValueService, ItemService }
import org.corespring.servicesAsync.metadata.{ MetadataService, MetadataSetService }
import org.corespring.servicesAsync._

trait Services {
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
