package org.corespring.player.accessControl.models.granter

import org.corespring.player.accessControl.models.{RequestedAccess, RenderOptions}

trait AccessGranter {
 def grant(request:RequestedAccess, options : RenderOptions ) : Boolean
}
