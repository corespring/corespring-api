package scorm.controllers

import play.api.templates.Html
import player.accessControl.auth.CheckPlayerSession
import player.controllers.Views
import player.views.models.PlayerParams
import common.controllers.AssetResource


object ScormPlayer extends Views(CheckPlayerSession) with AssetResource{
  override protected def defaultTemplate : (PlayerParams => Html) = (p:PlayerParams) => scorm.views.html.ScormPlayer(p)
}
