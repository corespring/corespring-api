package org.corespring.dev.tools.controllers

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.encryption.OrgEncrypter
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }

object Encrypter extends Controller {

  def encrypt(orgId: String) = Action { request =>
    val encrypter = new OrgEncrypter(new ObjectId(orgId), AESCrypto)

    for {
      json <- request.body.asJson
      result <- encrypter.encrypt(Json.stringify(json))
    } yield result

    Ok("")
  }
}
