package org.corespring.api.v1

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor
import org.bson.types.ObjectId
import org.corespring.api.v1.errors.ApiError
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.search.SearchCancelled
import org.corespring.platform.core.models.{ User, Organization }
import play.api.libs.json._
import play.api.mvc.Result
import scala.Left
import scala.Right
import scala.Some


/**
 * The User API
 */
object UserApi extends BaseApi {

  /**
   * Returns a list of users visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, optsort: Option[String]) = ApiActionRead { request =>

    def applySort(users: SalatMongoCursor[User]): Result = {
      optsort.map(User.toSortObj(_)) match {
        case Some(Right(sort)) => Ok(Json.toJson(users.sort(sort).skip(sk).limit(l).toSeq))
        case None => Ok(Json.toJson(users.skip(sk).limit(l).toSeq))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidSort(error.clientOutput)))
      }
    }

    val orgIds: Seq[ObjectId] = Organization.getTree(request.ctx.organization).map(_.id)
    val query = User.Dbo.orgIdIn(orgIds: _*)

    q.map(User.toSearchObj(_, Some(query))).getOrElse[Either[SearchCancelled, DBObject]](Right(query)) match {
      case Right(query) => f.map(User.toFieldsObj(_)) match {
        case Some(Right(searchFields)) => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(User.find(query).count))))
        else applySort(User.find(query, searchFields.dbfields))
        case None => if (c == "true") Ok(JsObject(Seq("count" -> JsNumber(User.find(query).count))))
        else applySort(User.find(query))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidFields(error.clientOutput)))
      }
      case Left(sc) => sc.error match {
        case None => Ok(JsArray(Seq()))
        case Some(error) => BadRequest(Json.toJson(ApiError.InvalidQuery(error.clientOutput)))
      }
    }
  }

  /**
   * Returns a User by its id
   *
   * @param id The user id
   * @return
   */
  def getUser(id: ObjectId) = ApiActionRead { request =>
    User.findOneById(id) match {
      case Some(user) => {
        val tree = Organization.getTree(request.ctx.organization)
        if (tree.exists(_.id == user.org.orgId)) {
          Ok(Json.toJson(user))
        } else Unauthorized
      }
      case _ => NotFound
    }
  }

  /**
   * Creates a user
   *
   * @return
   */
  def createUser = ApiActionWrite { request =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "id").asOpt[String] match {
          case Some(id) => BadRequest(Json.toJson(ApiError.IdNotNeeded))
          case _ => {
            val userInfo = for (
              userName <- (json \ "userName").asOpt[String];
              fullName <- (json \ "fullName").asOpt[String];
              email <- (json \ "email").asOpt[String];
              p <- Permission.fromLong((json \ "permissions").asOpt[Long].getOrElse(Permission.Read.value))
            ) yield ((userName, fullName, email, p))
            userInfo match {
              case Some((username, fullName, email, p)) => {
                val user = User(username, fullName, email)
                User.insertUser(user, request.ctx.organization, p, false) match {
                  case Right(u) => Ok(Json.toJson(u))
                  case Left(e) => InternalServerError(Json.toJson(ApiError.CreateUser(e.clientOutput)))
                }
              }
              case _ => BadRequest(Json.toJson(ApiError.UserRequiredFields))
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
  def updateUser(id: ObjectId) = ApiActionWrite { request =>
    User.findOneById(id).map(original =>
      {
        request.body.asJson match {
          case Some(json) => {
            val userName = (json \ "userName").asOpt[String].getOrElse(original.userName)
            val fullName = (json \ "fullName").asOpt[String].getOrElse(original.fullName)
            val email = (json \ "email").asOpt[String].getOrElse(original.email)
            User.updateUser(User(userName, fullName, email, id = id)) match {
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
