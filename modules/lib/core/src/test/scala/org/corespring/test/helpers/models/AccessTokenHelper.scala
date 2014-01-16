package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import scala.util.Random
import org.corespring.platform.core.models.auth.AccessToken

object AccessTokenHelper {

  def create(organizationId: ObjectId, userName: String): String = {
    val tokenId = randomString()
    AccessToken.insertToken(AccessToken(organizationId, scope = Some(userName), tokenId = tokenId, neverExpire = true))
    tokenId
  }

  def delete(tokenId: String) = AccessToken.removeToken(tokenId)

  val random = new scala.util.Random

  private def randomStringFromInput(alphabet: String)(n: Int): String =
    Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString

  def randomString(n: Int = 8) =
    randomStringFromInput("abcdefghijklmnopqrstuvwxyz0123456789")(n)

}
