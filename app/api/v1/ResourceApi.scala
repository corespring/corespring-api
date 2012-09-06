package api.v1

import play.api.mvc.Action
import controllers.auth.BaseApi

object ResourceApi extends BaseApi {

  def uploadFileToData(itemId:String, filename: String) = Action{
    NotImplemented
  }

  def uploadFile(itemId:String, materialName:String, fileName: String ) = Action {
    NotImplemented
  }

}
