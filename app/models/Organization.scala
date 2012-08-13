package models

import se.radley.plugin.salat._
import play.api.libs.json._
import play.api.libs.json.JsObject
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatRemoveError, SalatDAOUpdateError, ModelCompanion, SalatDAO}
import controllers.auth.Permission
import play.api.Play.current
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import models.mongoContext._
import play.api.Play
import controllers.{LogType, InternalError}

case class Organization(var name: String,
                        var path: Seq[ObjectId] = Seq(),
                        var contentcolls: Seq[ContentCollRef] = Seq(),
                        var id: ObjectId = new ObjectId()) {

  def this() = this("")
}

object Organization extends ModelCompanion[Organization, ObjectId] {
  val name: String = "name"
  val path: String = "path"
  val contentcolls: String = "contentcolls"

  val collection = mongoCollection("orgs")
  val dao = new SalatDAO[Organization, ObjectId](collection = collection) {}
  val queryFields = Map("name" -> "String")

  def apply(): Organization = new Organization();


  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[InternalError, Organization] = {
    if(Play.isProd) org.id = new ObjectId()
    optParentId match {
      case Some(parentId) => {
        findOneById(parentId) match {
          case Some(parent) => {
            org.path = parent.path :+ org.id
            insert(org) match {
              case Some(id) => Right(org)
              case None => Left(InternalError("error inserting organization",LogType.printFatal,true))
            }
          }
          case None => Left(InternalError("could not find parent given id",LogType.printError,true))
        }
      }
      case None => {
        org.path = Seq(org.id)
        insert(org) match {
          case Some(id) => Right(org)
          case None => Left(InternalError("error inserting organization",LogType.printFatal,true))
        }
      }
    }
  }

  def delete(orgId: ObjectId): Either[InternalError, Unit] = {
    try {
      remove(MongoDBObject(Organization.path -> orgId))
      Right(())
    } catch {
      case e:SalatRemoveError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to destroy organization tree")))
    }
  }

  def updateOrganization(org: Organization): Either[InternalError, Organization] = {
    try {
      Organization.update(MongoDBObject("_id" -> org.id), MongoDBObject("$set" -> MongoDBObject(name -> org.name)),
        false, false, Organization.collection.writeConcern)
      Organization.findOneById(org.id) match {
        case Some(org) => Right(org)
        case None => Left(InternalError("could not find organization that was just modified",LogType.printFatal,true))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("unable to update organization")))
    }
  }

  implicit object OrganizationWrites extends Writes[Organization] {
    def writes(org: Organization) = {
      JsObject(
        List(
          "id" -> JsString(org.id.toString),
          "name" -> JsString(org.name),
          "path" -> JsArray(org.path.map(c => JsString(c.toString)).toSeq)
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



