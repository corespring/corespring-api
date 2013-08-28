package scorm.controllers

import common.controllers.AssetResource
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.auth.CheckSessionAccess
import play.api.templates.Html
import player.controllers.Views
import player.views.models.PlayerParams
import org.corespring.platform.core.services.item.ItemServiceImpl

object ScormPlayer extends Views(CheckSessionAccess, ItemServiceImpl, QuizService) with AssetResource {
  override protected def defaultTemplate: (PlayerParams => Html) = (p: PlayerParams) => scorm.views.html.ScormPlayer(p)
}
