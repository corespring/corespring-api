package api.v1

import controllers.auth.BaseApi
import play.api.libs.json.Json
import models.User
import org.bson.types.ObjectId
import api.ApiError
import play.api.mvc.Result
import com.novus.salat.dao.SalatSaveError

/**
 * The User API
 */
object UserApi extends BaseApi {
  /**
   * Returns a list of users visible to the organization in the request context
   *
   * @return
   */
  def list() = ApiAction { request =>
    Ok(Json.toJson(User.findAllFor(request.ctx.organization).toList))
  }

  /**
   * Returns a User by its id
   *
   * @param id The user id
   * @return
   */
  def getUser(id: ObjectId) = ApiAction { request =>
    User.findOneById(id) match {
      case Some(org) =>  {
        // todo: check if this user is visible to the caller?
        Ok(Json.toJson(org))
      }
      case _ => NotFound
    }
  }

  /**
   * Creates a user
   *
   * @return
   */
  def createUser = ApiAction { request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val userInfo = for (
              userName <- (json \ "userName").asOpt[String] ;
              fullName <- (json \ "fullName").asOpt[String] ;
              email    <- (json \ "email").asOpt[String]
            ) yield ( (userName, fullName, email) )

            userInfo match {
              case Some((username, fullName, email)) => {
                val user = User(new ObjectId, username, fullName, email)
                doSave(user)
              }
              case _ => BadRequest( Json.toJson(ApiError.UserRequiredFields))
            }
          }
        }
      }
      case _ => jsonExpected
    }
  }

  /**
   * Updates a user
   *
   * @return
   */
  def updateUser(id: ObjectId) = ApiAction { request =>
    User.findOneById(id).map( original =>
    {
      request.body.asJson match {
        case Some(json) => {
          val userName = (json \ "userName").asOpt[String].getOrElse(original.userName)
          val fullName = (json \ "fullName").asOpt[String].getOrElse(original.fullName)
          val email    = (json \ "email").asOpt[String].getOrElse(original.email)
          doSave(User( original.id, userName, fullName, email))
        }
        case _ => jsonExpected
      }
    }).getOrElse(unknownUser)
  }

  /**
   * Deletes a user
   */
  def deleteUser(id: ObjectId) = ApiAction { request =>
    User.findOneById(id) match {
      case Some(toDelete) => {
        User.removeById(id)
        Ok(Json.toJson(toDelete))
      }
      case _ => unknownUser
    }
  }

  /**
   * Internal method to save a user
   *
   * @param user
   * @return
   */
  private def doSave(user: User): Result = {
    try {
      User.save(user)
      val newUser = User.findOneById(user.id)
      Ok(Json.toJson(newUser))
    } catch {
      case ex: SalatSaveError => InternalServerError(Json.toJson(ApiError.CantSave))
    }
  }

  private def unknownUser = NotFound(Json.toJson(ApiError.UnknownUser))

}
