package api.v1

import controllers.auth.{Permission, BaseApi}
import play.api.libs.json._
import models.{Organization, UserOrg, User}
import org.bson.types.ObjectId
import api.{ApiError}
import com.mongodb.casbah.Imports._
import play.api.mvc.Result
import controllers.Utils
import scala.Left
import models.search.SearchCancelled
import play.api.libs.json.JsArray
import scala.Some
import scala.Right
import com.novus.salat.dao.SalatMongoCursor
import play.api.libs.json.JsObject

/**
 * The User API
 */
object UserApi extends BaseApi {

  /**
   * Returns a list of users visible to the organization in the request context
   *
   * @return
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, optsort:Option[String]) = ApiActionRead { request =>
    val orgIds:Seq[ObjectId] = Organization.getTree(request.ctx.organization).map(_.id)
    val initSearch = MongoDBObject(User.orgs + "." + UserOrg.orgId -> MongoDBObject("$in" -> orgIds))
    def applySort(users:SalatMongoCursor[User]):Result = {
      optsort.map(User.toSortObj(_)) match {
        case Some(Right(sort)) => Ok(Json.toJson(Utils.toSeq(users.sort(sort).skip(sk).limit(l))))
        case None => Ok(Json.toJson(Utils.toSeq(users.skip(sk).limit(l))))
        case Some(Left(error)) => BadRequest(Json.toJson(ApiError.InvalidSort(error.clientOutput)))
      }
    }
    q.map(User.toSearchObj(_,Some(initSearch))).getOrElse[Either[SearchCancelled,MongoDBObject]](Right(initSearch)) match {
      case Right(query) => f.map(User.toFieldsObj(_)) match {
        case Some(Right(searchFields)) => if(c == "true") Ok(JsObject(Seq("count" -> JsNumber(User.find(query).count))))
                                          else applySort(User.find(query,searchFields.dbfields))
        case None => if(c == "true") Ok(JsObject(Seq("count" -> JsNumber(User.find(query).count))))
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
      case Some(user) =>  {
        val tree = Organization.getTree(request.ctx.organization)
        if(user.orgs.exists(uo => tree.exists(_.id == uo.orgId))){
          Ok(Json.toJson(user))
        }else Unauthorized
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
              userName <- (json \ "userName").asOpt[String] ;
              fullName <- (json \ "fullName").asOpt[String] ;
              email    <- (json \ "email").asOpt[String] ;
              p        <- Permission.fromLong((json \ "permissions").asOpt[Long].getOrElse(Permission.Read.value))
            ) yield ( (userName, fullName, email, p) )
            userInfo match {
              case Some((username, fullName, email, p)) => {
                val user = User(username, fullName, email)
                User.insertUser(user,request.ctx.organization,p,false) match {
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
  def updateUser(id: ObjectId) = ApiActionWrite { request =>
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
