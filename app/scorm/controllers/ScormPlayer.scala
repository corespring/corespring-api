package scorm.controllers

import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.auth.CheckSessionAccess
import play.api.templates.Html
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.player.v1.views.models.PlayerParams
import org.corespring.player.v1.controllers.Views
import org.corespring.platform.core.controllers.AssetResource

object ScormPlayer extends Views(CheckSessionAccess, ItemServiceImpl, QuizService) with AssetResource {
  override protected def defaultTemplate: (PlayerParams => Html) = (p: PlayerParams) => scorm.views.html.ScormPlayer(p)
}
