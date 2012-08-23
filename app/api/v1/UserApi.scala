package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json.Json
import models.{Organization, UserOrg, User}
import org.bson.types.ObjectId
import api.{QueryHelper, ApiError}
import com.novus.salat.dao.SalatMongoCursor
import com.mongodb.casbah.commons.MongoDBObject
import controllers.Utils

/**
 * The User API
 */
object UserApi extends BaseApi {
  /**
   * Returns a list of users visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = ApiAction { request =>
    val orgIds:List[ObjectId] = Organization.getTree(request.ctx.organization).map(_.id)
    val initSearch = MongoDBObject(User.orgs + "." + UserOrg.orgId -> MongoDBObject("$in" -> orgIds))
    QueryHelper.list[User, ObjectId](q, f, c, sk, l, User.queryFields, User.dao, User.UserWrites, Some(initSearch))
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
                val user = User(username, fullName, email)
                User.insertUser(user,request.ctx.organization,Permission.All,false) match {
                  case Right(u) => Ok(Json.toJson(u))
                  case Left(e) => InternalServerError(Json.toJson(ApiError.CreateUser(e.clientOutput)))
                }
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
          User.updateUser(User(userName,fullName,email,id = id)) match {
            case Right(u) => Ok(Json.toJson(u))
            case Left(e) => InternalServerError(Json.toJson(ApiError.UpdateUser(e.clientOutput)))
          }
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
        User.removeUser(id) match {
          case Right(_) => Ok(Json.toJson(toDelete))
          case Left(e) => InternalServerError(Json.toJson(ApiError.DeleteUser(e.clientOutput)))
        }
      }
      case _ => unknownUser
    }
  }

  def getUsersByOrg(orgId: ObjectId) = ApiAction { request =>
    NotImplemented
  }

  private def unknownUser = NotFound(Json.toJson(ApiError.UnknownUser))

}
