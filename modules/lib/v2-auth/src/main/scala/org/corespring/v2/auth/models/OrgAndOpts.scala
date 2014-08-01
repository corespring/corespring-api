package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization

case class OrgAndOpts(orgId: ObjectId, opts: PlayerOptions, org: Option[Organization] = None)

