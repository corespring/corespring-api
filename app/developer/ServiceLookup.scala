package developer

import org.corespring.models.json.JsonFormatting
import org.corespring.services.auth.ApiClientService
import org.corespring.services.item.ItemService
import org.corespring.services.{ RegistrationTokenService, ContentCollectionService, OrganizationService, UserService }

/**
 * An interim solution for parts of the application where we can't
 * inject dependencies.
 */
object ServiceLookup {

  var userService: UserService = null

  var orgService: OrganizationService = null

  var apiClientService: ApiClientService = null

  var contentCollectionService: ContentCollectionService = null

  var jsonFormatting: JsonFormatting = null

  var registrationTokenService: RegistrationTokenService = null

  var itemService: ItemService = null
}
