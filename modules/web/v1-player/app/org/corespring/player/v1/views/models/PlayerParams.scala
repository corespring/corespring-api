package org.corespring.player.v1.views.models

import org.corespring.qti.models.RenderingMode.RenderingMode
import org.corespring.web.common.controllers.deployment.{AssetsLoader, AssetsLoaderImpl}


case class PlayerParams(xml: String,
  itemId: Option[String],
  sessionId: Option[String] = None,
  role:String = "student",
  previewEnabled: Boolean = false,
  qtiKeys: QtiKeys,
  mode: RenderingMode,
  assetsLoader: AssetsLoader = AssetsLoaderImpl)

