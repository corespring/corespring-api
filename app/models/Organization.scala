package models

import se.radley.plugin.salat._
import play.api.libs.json._
import play.api.libs.json.JsObject
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAOUpdateError, ModelCompanion, SalatDAO}
import controllers.auth.Permission
import play.api.Play.current
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import models.mongoContext._

case class Organization(var name: String,
                        var path: Seq[ObjectId] = Seq(),
                        var contentcolls: Seq[ContentCollRef] = Seq(),
                        var id: ObjectId = new ObjectId()){

  def this() = this("")
}
object Organization extends ModelCompanion[Organization,ObjectId]{
  val name: String = "name"
  val path: String = "path"
  val contentcolls: String = "contentcolls"

  val collection = mongoCollection("orgs")

  val dao = new SalatDAO[Organization,ObjectId](collection = collection) {}
  def apply(): Organization = new Organization();


  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[ApiError,Organization] = {
    org.id = new ObjectId()
    optParentId match{
      case Some(parentId) => {
        findOneById(parentId) match {
          case Some(parent) => {
            org.path = parent.path :+ org.id
            insert(org) match {
              case Some(id) => Right(org)
              case None => Left(ApiError(ApiError.DatabaseError,"error inserting organization"))
            }
          }
          case None => Left(ApiError(ApiError.NotFound,"could not find parent given id"))
        }
      }
      case None => {
        org.path = Seq(org.id)
        insert(org) match {
          case Some(id) => Right(org)
          case None => Left(ApiError(ApiError.DatabaseError,"error inserting organization"))
        }
      }
    }
  }

  def delete(orgId: ObjectId): Either[ApiError,Unit] = {
    try{
      remove(MongoDBObject(Organization.path -> orgId))
      Right(())
    }catch{
      case e => Left(ApiError(ApiError.DatabaseError,e.getMessage))
    }
  }

  def updateOrganization(org:Organization):Either[ApiError,Organization] = {
    try{
      Organization.update(MongoDBObject("_id" -> org.id),MongoDBObject("$set" -> MongoDBObject(name -> org.name)),
        false, false, Organization.collection.writeConcern)
      Organization.findOneById(org.id) match {
        case Some(org) => Right(org)
        case None => Left(ApiError(ApiError.DatabaseError,"could not find organization that was just modified"))
      }
    }catch{
      case e:SalatDAOUpdateError => Left(ApiError(ApiError.DatabaseError,e.getMessage))
    }
  }

  implicit object OrganizationWrites extends Writes[Organization] {
    def writes(org: Organization) = {
      JsObject(
        List(
          "id" -> JsString(org.id.toString),
          "name" -> JsString(org.name),
          "path" -> JsArray(org.path.map( c => JsString(c.toString) ).toSeq)
        )
      )
    }
  }
}
case class ContentCollRef(var collId: ObjectId, var pval: Long = Permission.All.value)
object ContentCollRef {
  val pval: String = "pval"
  val collId: String = "collId"
}



