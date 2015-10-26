package org.corespring.models.auth

import org.bson.types.ObjectId

/**
 * An API client.  This gets created for each organization that is allowed API access
 */
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String)

