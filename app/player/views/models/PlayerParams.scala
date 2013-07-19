package player.views.models

import qti.models.RenderingMode.RenderingMode
import common.controllers.deployment.{AssetsLoaderImpl, AssetsLoader}

case class PlayerParams(xml: String,
                        itemId: Option[String],
                        sessionId: Option[String] = None,
                        previewEnabled: Boolean = false,
                        qtiKeys: QtiKeys,
                        mode: RenderingMode,
                        assetsLoader: AssetsLoader = AssetsLoaderImpl)


