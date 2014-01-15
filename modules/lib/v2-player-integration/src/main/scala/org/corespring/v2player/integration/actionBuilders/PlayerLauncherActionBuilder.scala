package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.PlayerJsRequest
import org.corespring.container.client.actions.{PlayerLauncherActionBuilder => Builder}
import org.corespring.v2player.integration.actionBuilders.access.{V2PlayerCookieWriter, PlayerOptions}
import play.api.mvc.Results._
import play.api.mvc._
import scala.Some
import scalaz.Scalaz._
import scalaz._

object PlayerLauncherActionBuilder{
  object Errors{
    val noClientId = "You must specify 'apiClient'"
    val noOptions = "You must specify 'options'"
    val noOrgId = "Error getting orgId"
    val cantDecrypt = "Error decrypting"
    val badJson = "Error reading json"
  }
}

trait PlayerLauncherActionBuilder extends Builder[AnyContent] with V2PlayerCookieWriter {

  def decrypt(orgId: ObjectId, encrypted: String): Option[String]

  def toOrgId(orgId: String): Option[ObjectId]

  protected def getOrgIdAndOptions(request:Request[AnyContent]) : Validation[String,(ObjectId,PlayerOptions)] = {
    for {
      apiClientId <- request.getQueryString("apiClient").toSuccess("You must specify 'apiClient'")
      encryptedOptions <- request.getQueryString("options").toSuccess("You must specify 'options'")
      orgId <- toOrgId(apiClientId).toSuccess("Error getting orgId")
      decryptedOptions <- decrypt(orgId, encryptedOptions).toSuccess("Error decrypting")
      playerOptions <- PlayerOptions.fromJson(decryptedOptions).toSuccess("Error reading json")
    } yield (orgId, playerOptions)
  }

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  override def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc +(kv._1, kv._2))
  }

  def playerJs(block: (PlayerJsRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      getOrgIdAndOptions(request) match {
        case Success((orgId, opts)) => {
          val newSession = sumSession(request.session, playerCookies(orgId, Some(opts)): _*)
          block(new PlayerJsRequest(opts.secure, request)).withSession(newSession)
        }
        case Failure(msg) => BadRequest(msg)
      }
  }
}
