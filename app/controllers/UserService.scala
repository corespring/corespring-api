package controllers

import auth.Permission
import org.bson.types.ObjectId
import models.{UserOrg, User, Organization}
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import dao.SalatMongoCursor
import scala.Left
import scala.Right
import scala.Some
import api.ApiError
import models.mongoContext._

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/7/12
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */

object UserService {

  def getUsers(orgId: ObjectId):Either[ApiError,Seq[User]] = {
    val c:SalatMongoCursor[User] = User.find(MongoDBObject(User.orgs+"."+UserOrg.orgId -> orgId))
    Right(c.toSeq)
  }
}
