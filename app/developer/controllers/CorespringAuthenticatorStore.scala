package developer.controllers

import securesocial.core.AuthenticatorStore
import play.api.Application
import securesocial.core.Authenticator
import play.api.cache.Cache
import play.api.Play.current
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models.mongoContext.context

object MongoAuthenticator extends ModelCompanion[Authenticator,ObjectId]{
  val collection = mongoCollection("sscache")
  def dao: DAO[Authenticator, ObjectId] = new SalatDAO[Authenticator,ObjectId](collection = collection) {}
}
class CorespringAuthenticatorStore(app: Application) extends AuthenticatorStore(app) {

  def save(authenticator: Authenticator): Either[Error, Unit] = {
    try{
      MongoAuthenticator.save(authenticator)
      Right(())
    }catch{
      case e:SalatSaveError =>Right(new Error(e.getMessage))
    }
    //Cache.set(authenticator.id, authenticator)
   // Right(())
  }
  def find(id: String): Either[Error, Option[Authenticator]] = {
    try {
      Right(MongoAuthenticator.findOne(MongoDBObject("_id" -> id)))
      //Right(Cache.getAs[Authenticator](id))
    } catch {
      case e: IllegalArgumentException => Left(new Error(e))
    }
  }
  def delete(id: String): Either[Error, Unit] = {
    try{
      MongoAuthenticator.remove(MongoDBObject("_id" -> id))
      Right(())
    }catch{
      case e:SalatRemoveError => Left(new Error(e.getMessage))
    }
//    Cache.set(id, "", 1)
//    Right(())
  }
}
