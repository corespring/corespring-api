package scorm.controllers

import testplayer.controllers.BasePlayer

object ScormPlayer extends BasePlayer {

  def OkRunPlayer(xml:String,itemId:String,sessionId:String,token:String) = {
    Ok(scorm.views.html.run(xml,itemId,sessionId,token))
  }
}
