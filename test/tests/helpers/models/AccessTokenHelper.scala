package tests.helpers.models

import org.corespring.platform.core.models.auth.AccessToken
import org.bson.types.ObjectId
import scala.util.Random

object AccessTokenHelper {

  def create(organizationId: ObjectId, userName: String): String = {
    val tokenId = randomString()
    AccessToken.insertToken(AccessToken(organizationId, scope = Some(userName), tokenId = tokenId))
    tokenId
  }

  def delete(tokenId: String) = AccessToken.removeToken(tokenId)

  private def randomString() = {
    val rnd = new Random()
    (for (i <- 0 until rnd.nextInt(64)) yield { ('0' + rnd.nextInt(64)).asInstanceOf[Char] }) mkString("")
  }

}
