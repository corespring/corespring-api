package org.corespring.legacy

import org.corespring.amazon.s3.S3Service
import org.corespring.models.json.JsonFormatting
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.assessment.{ AssessmentTemplateService, AssessmentService }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.item.ItemService
import org.corespring.services._
import org.corespring.services.metadata.{ MetadataSetService, MetadataService }

/**
 * An interim solution for parts of the application where we can't
 * inject dependencies.
 */
object ServiceLookup {

  var itemTransformer: ItemTransformer = null
  var s3Service: S3Service = null

  var metadataService: MetadataService = null

  var metadataSetService: MetadataSetService = null

  var standardService: StandardService = null

  var subjectService: SubjectService = null

  var assessmentTemplateService: AssessmentTemplateService = null

  var assessmentService: AssessmentService = null

  var userService: UserService = null

  var orgService: OrganizationService = null

  var apiClientService: ApiClientService = null

  var contentCollectionService: ContentCollectionService = null

  var accessTokenService: AccessTokenService = null

  var jsonFormatting: JsonFormatting = null

  var registrationTokenService: RegistrationTokenService = null

  var itemService: ItemService = null
}
