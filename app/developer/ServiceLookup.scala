package developer

import org.corespring.services.auth.ApiClientService
import org.corespring.services.{ContentCollectionService, OrganizationService, UserService}

/**
 * An interim solution for parts of the application where we can't
 * inject dependencies.
 */
object ServiceLookup {

  var userService : UserService = null

  var orgService : OrganizationService = null

  var apiClientService : ApiClientService = null

  var contentCollection : ContentCollectionService = null
}
