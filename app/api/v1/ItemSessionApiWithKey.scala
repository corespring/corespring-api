package api.v1

import controllers.auth.BaseRender
import com.mongodb.casbah.Imports._

object ItemSessionApiWithKey extends BaseRender{

  def aggregate(quizId: ObjectId, itemId: ObjectId) = RenderAction {request =>

    Ok
  }
}
