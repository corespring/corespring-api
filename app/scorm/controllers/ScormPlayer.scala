package scorm.controllers

import common.controllers.AssetResource
import play.api.templates.Html
import player.accessControl.auth.CheckPlayerSession
import player.controllers.Views
import player.views.models.PlayerParams


object ScormPlayer extends Views(CheckPlayerSession) with AssetResource{
  override protected def defaultTemplate : (PlayerParams => Html) = (p:PlayerParams) => scorm.views.html.ScormPlayer(p)
}
