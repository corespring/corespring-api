package basiclti.models.auth

import controllers.auth.RenderOptions

class LtiRenderOptions(collectionId:String,
                       override val itemId:String = "*",
                       override val sessionId:String = "*",
                        override val assessmentId : String = "*",
                        override val role : String = "*",
                        override val expires : Long = 0,
                        override val mode : String = "*") extends RenderOptions(itemId,sessionId,assessmentId,role,expires,mode)


