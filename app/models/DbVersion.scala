package models

import org.bson.types.ObjectId
import se.radley.plugin.salat._
import mongoContext._
import play.api.Play.current
import com.mongodb.casbah.Imports._
import com.novus.salat.dao._
import controllers.InternalError


case class DbVersion(version: String, id:ObjectId = new ObjectId())

object DbVersion extends ModelCompanion[DbVersion,ObjectId]{
  val version = "version"

  val collection = mongoCollection("dbversion")
  val dao = new SalatDAO[DbVersion, ObjectId](collection = collection) {}

  def getVersion:String = {
    findOne(MongoDBObject()).map(_.version).getOrElse("0.0.0")
  }
  def updated(updatedVersion:String):Either[InternalError,Unit] = {
    findOne(MongoDBObject()) match {
      case Some(oldVersion) => try{
        update(MongoDBObject("_id" -> oldVersion.id), MongoDBObject("$set" -> MongoDBObject(version -> updatedVersion)),false,false,dao.defaultWriteConcern)
        Right(())
      }catch{
        case e:SalatDAOUpdateError => Left(InternalError("error occurred when updating new db version"))
      }
      case None => try {
        insert(DbVersion(updatedVersion)) match {
          case Some(_) => Right(())
          case None =>Left(InternalError("error occured when inserting new db version"))
        }
      }catch{
        case e:SalatInsertError => Left(InternalError("error occured when inserting new db version"))
      }
    }
  }
}
