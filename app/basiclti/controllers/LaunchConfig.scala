package basiclti.controllers

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import basiclti.models.LtiLaunchConfiguration
import basiclti.models.LtiLaunchConfiguration._

import play.api.libs.json.Json._
import play.api.mvc.AnyContent
import play.api.mvc.Request

object LaunchConfig extends BaseApi {

  def get(id: ObjectId) = ApiAction {
    request =>

      LtiLaunchConfiguration.findOneById(id) match {
        case Some(c) => Ok(toJson(c))
        case _ => NotFound("Can't find launch config with that id")
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>

      println(request.body)
      config(request) match {
        case Some(config) => {
          if (id != config.id) {
            BadRequest("the json id doesn't match the url id")
          } else {
            LtiLaunchConfiguration.update(config) match {
              case Left(e) => BadRequest("Error updating")
              case Right(updatedConfig) => {
                Ok(toJson(updatedConfig))
              }
            }
          }
        }
        case _ => BadRequest("Invalid json provided")
      }
  }

  private def config(request : Request[AnyContent]) : Option[LtiLaunchConfiguration] = {
    request.body.asJson match {
      case Some(json) => {
        try {
          val out = json.asOpt[LtiLaunchConfiguration]
          out
        }
        catch {
          case e : Throwable => {
            play.Logger.warn(e.getMessage)
            None
          }
        }
      }
      case _ => None
    }
  }
}
