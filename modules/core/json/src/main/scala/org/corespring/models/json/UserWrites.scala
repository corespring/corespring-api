package org.corespring.models.json

import org.corespring.models.User
import play.api.libs.json.{ JsObject, JsString, Writes }

object UserWrites extends Writes[User] {
  def writes(user: User) = {
    var list = List[(String, JsString)]()
    if (user.email.nonEmpty) list = ("email" -> JsString(user.email)) :: list
    if (user.fullName.nonEmpty) list = ("fullName" -> JsString(user.fullName)) :: list
    if (user.userName.nonEmpty) list = ("userName" -> JsString(user.userName)) :: list
    list = "id" -> JsString(user.id.toString) :: list
    JsObject(list)
  }
}

