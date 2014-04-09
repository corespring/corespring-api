package player.views.models

import org.corespring.qti.models.RenderingMode.RenderingMode
import org.corespring.web.common.controllers.deployment.{ AssetsLoaderImpl, AssetsLoader }

case class PlayerParams(xml: String,
  itemId: Option[String],
  sessionId: Option[String] = None,
  role:String = "student",
  previewEnabled: Boolean = false,
  qtiKeys: QtiKeys,
  mode: RenderingMode,
  assetsLoader: AssetsLoader = AssetsLoaderImpl,
  rawQueryString: Option[String] = None)

