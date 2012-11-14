package scorm.controllers

import controllers.auth.BaseApi
import org.bson.types.ObjectId

object ScormPlayer extends BaseApi {

  def run(itemId:ObjectId) = ApiAction{ request =>

    Ok(scorm.views.html.run())

  }
}
