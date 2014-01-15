package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{PlayerLauncherActionBuilder => Builder, PlayerJsRequest}
import org.corespring.v2player.integration.actionBuilders.access.{V2PlayerCookieWriter, PlayerOptions}
import play.api.mvc.Results._
import play.api.mvc.{Session, AnyContent, Action, Result}
import scalaz.Scalaz._
import scalaz._

trait PlayerLauncherActionBuilder extends Builder[AnyContent] with V2PlayerCookieWriter {

  def decrypt(s: String): Option[String]

  def toOrgId(apiClientId: String): Option[ObjectId]

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  override def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc +(kv._1, kv._2))
  }

  def playerJs(block: (PlayerJsRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      val result: Validation[String, (ObjectId, PlayerOptions)] = for {
        apiClientId <- request.getQueryString("apiClient").toSuccess("You must specify 'apiClient'")
        encryptedOptions <- request.getQueryString("options").toSuccess("You must specify 'options'")
        decryptedOptions <- decrypt(encryptedOptions).toSuccess("Error decrypting")
        orgId <- toOrgId(apiClientId).toSuccess("Error getting orgId")
        playerOptions <- PlayerOptions.fromJson(decryptedOptions).toSuccess("Error reading json")
      } yield (orgId, playerOptions)

      result match {
        case Success((orgId, opts)) => {
          val newSession = sumSession(request.session, playerCookies(orgId, Some(opts)): _*)
          block(new PlayerJsRequest(opts.secure, request)).withSession(newSession)
        }
        case Failure(msg) => BadRequest(msg)
      }
  }
}
