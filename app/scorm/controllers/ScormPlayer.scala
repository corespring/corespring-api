package scorm.controllers

import common.controllers.AssetResource
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.player.accessControl.auth.CheckSessionAccess
import play.api.templates.Html
import player.controllers.Views
import player.views.models.PlayerParams
import org.corespring.platform.core.services.item.ItemServiceImpl

object ScormPlayer extends Views(CheckSessionAccess, ItemServiceImpl, AssessmentService) with AssetResource {
  override protected def defaultTemplate: (PlayerParams => Html) = (p: PlayerParams) => scorm.views.html.ScormPlayer(p)
}
