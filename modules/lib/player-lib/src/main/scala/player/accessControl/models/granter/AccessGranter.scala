package player.accessControl.models.granter

import player.accessControl.models.{RenderOptions, RequestedAccess}

trait AccessGranter {
 def grant(request:RequestedAccess, options : RenderOptions ) : Boolean
}
