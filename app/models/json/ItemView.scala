package models.json

import models.{Standard, Subject, ContentType, Item}
import models.search.SearchFields
import play.api.libs.json._
import scala.Some
import models.search.SearchFields
import scala.Some
import models.search.SearchFields
import scala.Some
import models.search.SearchFields
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import scala.Some
import models.search.SearchFields
import play.api.Logger
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsNumber
import models.search.SearchFields
import models.Workflow.WorkflowWrites

case class ItemView(item:Item, searchFields:Option[SearchFields])
object ItemView{
  implicit object ItemViewWrites extends Writes[ItemView]{
    def writes(itemView:ItemView) = {
      itemView.searchFields.foreach(_.addDbFieldsToJsFields)

      def checkFields(key:String):Boolean = {
        if (itemView.searchFields.isDefined){
          if(itemView.searchFields.get.inclusion){
            itemView.searchFields.get.jsfields.exists(_ == key)
          }else{
            itemView.searchFields.get.jsfields.exists(_ != key)
          }
        } else true
      }
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(itemView.item.id.toString))

      if (itemView.item.workflow.isDefined) iseq = iseq :+ (Item.workflow -> WorkflowWrites.writes(itemView.item.workflow.get))

      //ContributorDetails
      itemView.item.contributorDetails match {
        case Some(cd) => {
          cd.author.foreach(v => if (checkFields(Item.author)) iseq = iseq :+ (Item.author -> JsString(v)))
          cd.contributor.foreach(v => if (checkFields(Item.contributor)) iseq = iseq :+ (Item.contributor -> JsString(v)))
          cd.costForResource.foreach(v => if (checkFields(Item.costForResource)) iseq = iseq :+ (Item.costForResource -> JsNumber(v)))
          cd.credentials.foreach(v => if (checkFields(Item.credentials)) iseq = iseq :+ (Item.credentials -> JsString(v)))
          cd.licenseType.foreach(v => if (checkFields(Item.licenseType)) iseq = iseq :+ (Item.licenseType -> JsString(v)))
          cd.sourceUrl.foreach(v => if (checkFields(Item.sourceUrl)) iseq = iseq :+ (Item.sourceUrl -> JsString(v)))
          cd.copyright match {
            case Some(c) => {
              c.owner.foreach(v => if (checkFields(Item.copyrightOwner)) iseq = iseq :+ (Item.copyrightOwner -> JsString(v)))
              c.year.foreach(v => if (checkFields(Item.copyrightYear)) iseq = iseq :+ (Item.copyrightYear -> JsString(v)))
              c.expirationDate.foreach(v => if (checkFields(Item.copyrightExpirationDate)) iseq = iseq :+ (Item.copyrightExpirationDate -> JsString(v)))
              c.imageName.foreach(v => if (checkFields(Item.copyrightImageName)) iseq = iseq :+ (Item.copyrightImageName -> JsString(v)))
            }
            case _ => //do nothing
          }
        }
        case _ => //do nothing
      }

      itemView.item.lexile.foreach(v => if (checkFields(Item.lexile)) iseq = iseq :+ (Item.lexile -> JsString(v)))

      itemView.item.demonstratedKnowledge.foreach(v => if (checkFields(Item.demonstratedKnowledge)) iseq = iseq :+ (Item.demonstratedKnowledge -> JsString(v)))

      itemView.item.originId.foreach(v => if (checkFields(Item.originId)) iseq = iseq :+ (Item.originId -> JsString(v)))

      if (checkFields(Item.collectionId)) iseq = iseq :+ (Item.collectionId -> JsString(itemView.item.collectionId))
      if (checkFields(Item.contentType)) iseq = iseq :+ (Item.contentType -> JsString(ContentType.item))
      itemView.item.pValue.foreach(v => if (checkFields(Item.pValue)) iseq = iseq :+ (Item.pValue -> JsString(v)))
      itemView.item.relatedCurriculum.foreach(v => if (checkFields(Item.relatedCurriculum)) iseq = iseq :+ (Item.relatedCurriculum -> JsString(v)))

      itemView.item.bloomsTaxonomy.foreach(v => if(checkFields(Item.bloomsTaxonomy)) iseq = iseq :+ (Item.bloomsTaxonomy -> JsString(v)))

      if (!itemView.item.supportingMaterials.isEmpty && checkFields(Item.supportingMaterials))
        iseq = iseq :+ (Item.supportingMaterials -> JsArray(itemView.item.supportingMaterials.map(Json.toJson(_))))
      if (!itemView.item.gradeLevel.isEmpty && checkFields(Item.gradeLevel))
        iseq = iseq :+ (Item.gradeLevel -> JsArray(itemView.item.gradeLevel.map(JsString(_))))
      itemView.item.itemType.foreach(v => if (checkFields(Item.itemType)) iseq = iseq :+ (Item.itemType -> JsString(v)))
      if (!itemView.item.keySkills.isEmpty && checkFields(Item.keySkills)) iseq = iseq :+ (Item.keySkills -> JsArray(itemView.item.keySkills.map(JsString(_))))


      itemView.item.subjects match {
        case Some(s) => {

          def getSubject(id: Option[ObjectId]): Option[JsValue] = id match {
            case Some(foundId) => {
              Subject.findOneById(foundId) match {
                case Some(subj) => Some(Json.toJson(subj))
                case _ => throw new RuntimeException("Can't find subject with id: " + foundId + " in itemView.item: " + itemView.item.id)
              }
            }
            case _ => None
          }

          var seqsubjects: Seq[(String, JsValue)] = Seq()

          getSubject(s.primary) match {
            case Some(found) => if (checkFields(Item.primarySubject)) iseq = iseq :+ (Item.primarySubject -> Json.toJson(found))
            case _ => //do nothing
          }
          getSubject(s.related) match {
            case Some(found) => if (checkFields(Item.relatedSubject)) iseq = iseq :+ (Item.relatedSubject -> Json.toJson(found))
            case _ => //do nothing
          }
        }
        case _ => //
      }

      itemView.item.priorUse.foreach(v => if (checkFields(Item.priorUse)) iseq = iseq :+ (Item.priorUse -> JsString(v)))
      if (!itemView.item.priorGradeLevel.isEmpty && checkFields(Item.priorGradeLevel)) iseq = iseq :+ (Item.priorGradeLevel -> JsArray(itemView.item.priorGradeLevel.map(JsString(_))))
      if (!itemView.item.reviewsPassed.isEmpty && checkFields(Item.reviewsPassed)) iseq = iseq :+ (Item.reviewsPassed -> JsArray(itemView.item.reviewsPassed.map(JsString(_))))
      if (!itemView.item.standards.isEmpty && checkFields(Item.standards)) iseq = iseq :+ (Item.standards -> Json.toJson(itemView.item.standards.
        foldRight[Seq[Standard]](Seq[Standard]())((dn, acc) => Standard.findOne( MongoDBObject("dotNotation" -> dn)) match {
        case Some(standard) => acc :+ standard
        case None => {
          //throw new RuntimeException("ItemWrites: no standard found given id: " + sid); acc
          Logger.warn("no standard found for id: " + dn + ", itemView.item id: " + itemView.item.id )
          acc
        }
      })))
      itemView.item.title.foreach(v => if (checkFields(Item.title))iseq = iseq :+ (Item.title -> JsString(v)))
      itemView.item.data.foreach(v => if (checkFields(Item.data)) iseq = iseq :+ (Item.data -> Json.toJson(v)))
      JsObject(iseq)
    }
  }
}
