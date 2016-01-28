package org.corespring.models.auth

import org.bson.types.ObjectId

/**
 * An API client.  This gets created for each organization that is allowed API access
 * TODO: rm clientId - just use id
 */
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String)

