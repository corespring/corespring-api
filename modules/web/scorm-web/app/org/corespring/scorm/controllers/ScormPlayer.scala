package org.corespring.scorm.controllers

import org.corespring.platform.core.controllers.AssetResource
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.auth.CheckSessionAccess
import org.corespring.player.v1.controllers.Views
import org.corespring.player.v1.views.models.PlayerParams
import play.api.templates.Html

object ScormPlayer extends Views(CheckSessionAccess, ItemServiceWired, QuizService) with AssetResource {
  override protected def defaultTemplate: (PlayerParams => Html) = (p: PlayerParams) => org.corespring.scorm.views.html.ScormPlayer(p)
}
