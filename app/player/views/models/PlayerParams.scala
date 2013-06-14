package player.views.models

import qti.models.RenderingMode.RenderingMode

case class PlayerParams(xml: String,
                        itemId: Option[String],
                        sessionId: Option[String] = None,
                        previewEnabled: Boolean = false,
                        qtiKeys: QtiKeys,
                        mode: RenderingMode)


