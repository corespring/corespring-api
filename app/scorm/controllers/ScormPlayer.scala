package scorm.controllers

import common.controllers.AssetResource
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import org.corespring.platform.core.models.quiz.basic.Quiz
import play.api.templates.Html
import player.controllers.Views
import player.views.models.PlayerParams
import org.corespring.player.accessControl.auth.CheckSessionAccess


object ScormPlayer extends Views(CheckSessionAccess, ItemServiceImpl, Quiz) with AssetResource{
  override protected def defaultTemplate : (PlayerParams => Html) = (p:PlayerParams) => scorm.views.html.ScormPlayer(p)
}
