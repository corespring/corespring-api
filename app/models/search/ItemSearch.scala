package models.search

import com.mongodb.casbah.Imports._
import controllers.InternalError
import controllers.Utils
import models._
import models.item.Item.Keys._
import models.item._
import scala.Left
import scala.Right
import scala.Some


object ItemSearch extends Searchable {

  override protected def toFieldsObjInternal(dbfields: BasicDBObject, method: Int): Either[InternalError, SearchFields] = {
    def toSearchFieldObj(searchFields: SearchFields, field: (String, AnyRef), addToFieldsObj: Boolean = true, dbkey: String = ""): Either[InternalError, SearchFields] = {
      if (field._2 == method) {
        if (addToFieldsObj) {
          //if(dbkey.isEmpty) field._1 else dbkey
          searchFields.dbfields = searchFields.dbfields += ((if (dbkey.isEmpty) field._1 else dbkey) -> field._2)
        }
        searchFields.jsfields = searchFields.jsfields :+ field._1
        Right(searchFields)
      } else {
        Left(InternalError("Wrong value for " + field._1 + ". Should have been " + method))
      }
    }

    dbfields.foldRight[Either[InternalError, SearchFields]](Right(SearchFields(method = method)))((field, result) => result match {
      case Right(searchFields) => {
        field._1 match {
          case "id" => toSearchFieldObj(searchFields, field)
          case key if key.startsWith(workflow) => toSearchFieldObj(searchFields, field)
          case `author` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.author)
          case `contributor` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.contributor)
          case `costForResource` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.costForResource)
          case `credentials` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.credentials)
          case `licenseType` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.licenseType)
          case `sourceUrl` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.sourceUrl)
          case `copyrightOwner` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.owner)
          case `copyrightYear` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.year)
          case `copyrightExpirationDate` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.expirationDate)
          case `copyrightImageName` => toSearchFieldObj(searchFields, field, dbkey = contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.imageName)
          case `lexile` => toSearchFieldObj(searchFields, field)
          case `demonstratedKnowledge` => toSearchFieldObj(searchFields, field, dbkey = otherAlignments + "." + Alignments.Keys.demonstratedKnowledge)
          case `originId` => toSearchFieldObj(searchFields, field)
          case `collectionId` => toSearchFieldObj(searchFields, field)
          case `contentType` => toSearchFieldObj(searchFields, field)
          case `pValue` => toSearchFieldObj(searchFields, field)
          case `relatedCurriculum` => toSearchFieldObj(searchFields, field, dbkey = otherAlignments + "." + Alignments.Keys.relatedCurriculum)
          case `gradeLevel` => toSearchFieldObj(searchFields, field, dbkey = taskInfo + "." + TaskInfo.Keys.gradeLevel)
          case `itemType` => toSearchFieldObj(searchFields, field, dbkey = taskInfo + "." + TaskInfo.Keys.itemType)
          case `keySkills` => toSearchFieldObj(searchFields, field, dbkey = otherAlignments + "." + Alignments.Keys.keySkills)
          case `bloomsTaxonomy` => toSearchFieldObj(searchFields, field, dbkey = otherAlignments + "." + Alignments.Keys.bloomsTaxonomy)
          case `primarySubject` => toSearchFieldObj(searchFields, field, dbkey = taskInfo + "." + TaskInfo.Keys.subjects + "." + Subjects.Keys.primary)
          case key if key.startsWith(primarySubject) => toSearchFieldObj(searchFields, field, false)
          case `relatedSubject` => toSearchFieldObj(searchFields, field, dbkey = subjects + "." + Subjects.Keys.related)
          case key if key.startsWith(relatedSubject) => toSearchFieldObj(searchFields, field, false)
          case `priorUse` => toSearchFieldObj(searchFields, field)
          case `priorGradeLevel` => toSearchFieldObj(searchFields, field)
          case `reviewsPassed` => toSearchFieldObj(searchFields, field)
          case `standards` => toSearchFieldObj(searchFields, field)
          case key if key.startsWith(standards) => toSearchFieldObj(searchFields, field, false)
          case `title` => toSearchFieldObj(searchFields, field, dbkey = taskInfo + "." + TaskInfo.Keys.title)
          case `published` => toSearchFieldObj(searchFields, field)
          case key if key.startsWith(extended) => toSearchFieldObj(searchFields,field,dbkey = taskInfo+"."+key)
          case _ => Left(InternalError("unknown key contained in fields: " + field._1))
        }
      }
      case Left(e) => Left(e)
    })
  }

  private def preParseSubjects(dbquery: BasicDBObject)(implicit parseFields: Map[String, (AnyRef) => Either[InternalError, AnyRef]]): Either[SearchCancelled, MongoDBObject] = {
    val primarySubjectQuery = dbquery.foldRight[Either[SearchCancelled, MongoDBObject]](Right(MongoDBObject()))((field, result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == primarySubject + "." + Subject.Subject =>
            formatQuery(Subject.Subject, field._2, searchobj)
          case key if key == primarySubject + "." + Subject.Category =>
            formatQuery(Subject.Category, field._2, searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty) {
        val subjects = Subject.find(searchobj).toSeq.map(_.id)
        if (subjects.size > 1) Right(MongoDBObject(taskInfo + "." + TaskInfo.Keys.subjects + "." + Subjects.Keys.primary -> MongoDBObject("$in" -> subjects)))
        else if (subjects.size == 1) Right(MongoDBObject(taskInfo + "." + TaskInfo.Keys.subjects + "." + Subjects.Keys.primary -> subjects.head))
        else Left(SearchCancelled(None))
      } else Right(MongoDBObject())
      case Left(sc) => Left(sc)
    }
    val relatedSubjectQuery = dbquery.foldRight[Either[SearchCancelled, MongoDBObject]](Right(MongoDBObject()))((field, result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == relatedSubject + "." + Subject.Subject =>
            formatQuery(Subject.Subject, field._2, searchobj)
          case key if key == relatedSubject + "." + Subject.Category =>
            formatQuery(Subject.Category, field._2, searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty) {
        val subjects: Seq[ObjectId] = Subject.find(searchobj).toSeq.map(_.id)
        if (subjects.size > 1) Right(MongoDBObject(taskInfo + "." + TaskInfo.Keys.subjects + "." + Subjects.Keys.related -> MongoDBObject("$in" -> subjects)))
        else if (subjects.size == 1) Right(MongoDBObject(taskInfo + "." + TaskInfo.Keys.subjects + "." + Subjects.Keys.related -> subjects.head))
        else Left(SearchCancelled(None))
      } else Right(MongoDBObject())
      case Left(sc) => Left(sc)
    }
    primarySubjectQuery match {
      case Right(psq) => relatedSubjectQuery match {
        case Right(rsq) => Right(psq ++ rsq)
        case Left(sc) => Left(sc)
      }
      case Left(sc) => Left(sc)
    }
  }

  private def preParseStandards(dbquery: BasicDBObject)(implicit parseFields: Map[String, (AnyRef) => Either[InternalError, AnyRef]]): Either[SearchCancelled, MongoDBObject] = {
    dbquery.foldRight[Either[SearchCancelled, MongoDBObject]](Right(MongoDBObject()))((field, result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == standards + "." + Standard.DotNotation => formatQuery(Standard.DotNotation, field._2, searchobj)
          case key if key == standards + "." + Standard.guid => formatQuery(Standard.guid, field._2, searchobj)
          case key if key == standards + "." + Standard.Subject => formatQuery(Standard.Subject, field._2, searchobj)
          case key if key == standards + "." + Standard.Category => formatQuery(Standard.Category, field._2, searchobj)
          case key if key == standards + "." + Standard.SubCategory => formatQuery(Standard.SubCategory, field._2, searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty) {
        val standards: Seq[String] = Standard.find(searchobj).toSeq.map(_.dotNotation).flatten
        if (standards.nonEmpty) {
          if (standards.size == 1) Right(MongoDBObject( Item.Keys.standards -> standards.head))
          else Right(MongoDBObject(Item.Keys.standards -> MongoDBObject("$in" -> standards)))
        } else Left(SearchCancelled(None))
      } else Right(MongoDBObject())
      case Left(sc) => Left(sc)
    }
  }
  override protected def toSearchObjInternal(dbquery: BasicDBObject, optInitSearch: Option[MongoDBObject])(implicit parseFields: Map[String, (AnyRef) => Either[InternalError, AnyRef]]): Either[SearchCancelled, MongoDBObject] = {
    preParseStandards(dbquery) match {
      case Right(query1) => preParseSubjects(dbquery) match {
        case Right(query2) => {
          val initSearch = query1 ++ query2.asDBObject ++ optInitSearch.getOrElse[MongoDBObject](MongoDBObject()).asDBObject
          dbquery.foldRight[Either[SearchCancelled, MongoDBObject]](Right(initSearch))((field, result) => result match {
            case Right(searchobj) => {
              field._1 match {
                case key if key == workflow + "." + Workflow.setup => formatQuery(key, field._2, searchobj)
                case key if key == workflow + "." + Workflow.tagged => formatQuery(key, field._2, searchobj)
                case key if key == workflow + "." + Workflow.standardsAligned => formatQuery(key, field._2, searchobj)
                case key if key == workflow + "." + Workflow.qaReview => formatQuery(key, field._2, searchobj)
                case `author` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.author, field._2, searchobj)
                case `contributor` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.contributor, field._2, searchobj)
                case `costForResource` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.costForResource, field._2, searchobj)
                case `credentials` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.credentials, field._2, searchobj)
                case `licenseType` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.licenseType, field._2, searchobj)
                case `sourceUrl` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.sourceUrl, field._2, searchobj)
                case `copyrightOwner` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.owner, field._2, searchobj)
                case `copyrightYear` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.year, field._2, searchobj)
                case `copyrightExpirationDate` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.expirationDate, field._2, searchobj)
                case `copyrightImageName` => formatQuery(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.imageName, field._2, searchobj)
                case `lexile` => formatQuery(lexile, field._2, searchobj)
                case `demonstratedKnowledge` => formatQuery(otherAlignments + "." + Alignments.Keys.demonstratedKnowledge, field._2, searchobj)
                case `originId` => formatQuery(originId, field._2, searchobj)
                case `collectionId` => formatQuery(collectionId, field._2, searchobj)
                case `published` => formatQuery(published, field._2, searchobj)
                case `contentType` => Right(searchobj)
                case `pValue` => formatQuery(pValue, field._2, searchobj)
                case `relatedCurriculum` => formatQuery(otherAlignments + "." + Alignments.Keys.relatedCurriculum, field._2, searchobj)
                case `supportingMaterials` => Left(SearchCancelled(Some(InternalError("cannot query on supportingMaterials"))))
                case `gradeLevel` => formatQuery(taskInfo + "." + TaskInfo.Keys.gradeLevel, field._2, searchobj)
                case `itemType` => formatQuery(taskInfo + "." + TaskInfo.Keys.itemType, field._2, searchobj)
                case `keySkills` => formatQuery(otherAlignments + "." + Alignments.Keys.keySkills, field._2, searchobj)
                case `bloomsTaxonomy` => formatQuery(otherAlignments + "." + Alignments.Keys.bloomsTaxonomy, field._2, searchobj)
                case key if key.startsWith(primarySubject) => Right(searchobj)
                case key if key == key.startsWith(relatedSubject) => Right(searchobj)
                case `priorUse` => formatQuery(priorUse, field._2, searchobj)
                case `priorGradeLevel` => formatQuery(priorGradeLevel, field._2, searchobj)
                case `reviewsPassed` => formatQuery(reviewsPassed, field._2, searchobj)
                case key if key.startsWith(standards) => Right(searchobj)
                case `title` => formatQuery(taskInfo + "." + TaskInfo.Keys.title, field._2, searchobj)
                case key if key.startsWith(extended) => formatQuery(taskInfo+"."+key,field._2,searchobj)
                case _ => Left(SearchCancelled(Some(InternalError("unknown key contained in query: " + field._1))))
              }
            }
            case Left(e) => Left(e)
          })
        }
        case Left(sc) => Left(sc)
      }
      case Left(sc) => Left(sc)
    }
  }

  override protected def toSortObjInternal(field: (String, AnyRef)): Either[InternalError, MongoDBObject] = {
    def formatSortField(key: String, value: AnyRef): Either[InternalError, MongoDBObject] = {
      value match {
        case intval: java.lang.Integer => Right(MongoDBObject(key -> value))
        case _ => Left(InternalError("sort value not a number"))
      }
    }
    field._1 match {
      case key if key == workflow + "." + Workflow.setup => formatSortField(key, field._2)
      case key if key == workflow + "." + Workflow.tagged => formatSortField(key, field._2)
      case key if key == workflow + "." + Workflow.standardsAligned => formatSortField(key, field._2)
      case key if key == workflow + "." + Workflow.qaReview => formatSortField(key, field._2)
      case `author` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.author, field._2)
      case `contributor` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.contributor, field._2)
      case `costForResource` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.costForResource, field._2)
      case `credentials` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.credentials, field._2)
      case `licenseType` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.licenseType, field._2)
      case `sourceUrl` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.sourceUrl, field._2)
      case `copyrightOwner` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.owner, field._2)
      case `copyrightYear` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.year, field._2)
      case `copyrightExpirationDate` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.expirationDate, field._2)
      case `copyrightImageName` => formatSortField(contributorDetails + "." + ContributorDetails.Keys.copyright + "." + Copyright.Keys.imageName, field._2)
      case `lexile` => formatSortField(lexile, field._2)
      case `demonstratedKnowledge` => formatSortField(otherAlignments + "." + Alignments.Keys.demonstratedKnowledge, field._2)
      case `originId` => formatSortField(originId, field._2)
      case `contentType` => formatSortField(contentType, field._2)
      case `pValue` => formatSortField(pValue, field._2)
      case `relatedCurriculum` => formatSortField(otherAlignments + "." + Alignments.Keys.relatedCurriculum, field._2)
      case `gradeLevel` => formatSortField(taskInfo + "." + TaskInfo.Keys.gradeLevel, field._2)
      case `itemType` => formatSortField(taskInfo + "." + TaskInfo.Keys.itemType, field._2)
      case `keySkills` => formatSortField(otherAlignments + "." + Alignments.Keys.keySkills, field._2)
      case `bloomsTaxonomy` => formatSortField(otherAlignments + "." + Alignments.Keys.bloomsTaxonomy, field._2)
      case `priorUse` => formatSortField(priorUse, field._2)
      case `priorGradeLevel` => formatSortField(priorGradeLevel, field._2)
      case `reviewsPassed` => formatSortField(reviewsPassed, field._2)
      case key if key == standards + "." + Standard.DotNotation => formatSortField(standards, field._2)
      case `title` => formatSortField(taskInfo + "." + TaskInfo.Keys.title, field._2)
      case `collectionId` => formatSortField(collectionId, field._2)
      case `published` => formatSortField(published, field._2)
      case key if key.startsWith(extended) => formatSortField(taskInfo+"."+key,field._2)
      case _ => Left(InternalError("unknown or invalid key contained in sort field"))
    }
  }

}
