package models.search

import controllers.{Utils}
import com.mongodb.casbah.Imports._
import models._
import item._
import controllers.InternalError
import scala.Left
import scala.Right
import scala.Some

object ItemSearch extends Searchable{

  override protected def toFieldsObjInternal(dbfields:BasicDBObject,method:Int):Either[InternalError,SearchFields] = {
    def toSearchFieldObj(searchFields:SearchFields,field:(String,AnyRef),addToFieldsObj:Boolean = true,dbkey:String=""):Either[InternalError,SearchFields] = {
      if(field._2 == method){
        if(addToFieldsObj) {
          //if(dbkey.isEmpty) field._1 else dbkey
          searchFields.dbfields = searchFields.dbfields += ((if(dbkey.isEmpty) field._1 else dbkey) -> field._2)
        }
        searchFields.jsfields = searchFields.jsfields :+ field._1
        Right(searchFields)
      }else{
        Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
      }
    }
    dbfields.foldRight[Either[InternalError,SearchFields]](Right(SearchFields(method = method)))((field,result) => result match {
      case Right(searchFields) => {
        field._1 match {
          case "id" => toSearchFieldObj(searchFields,field)
          case key if key.startsWith(Item.workflow) => toSearchFieldObj(searchFields,field)
          case Item.author => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.author)
          case Item.contributor => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.contributor)
          case Item.costForResource => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.costForResource)
          case Item.credentials => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.credentials)
          case Item.licenseType => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.licenseType)
          case Item.sourceUrl => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.sourceUrl)
          case Item.copyrightOwner => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.owner)
          case Item.copyrightYear => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.year)
          case Item.copyrightExpirationDate => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.expirationDate)
          case Item.copyrightImageName => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.imageName)
          case Item.lexile => toSearchFieldObj(searchFields,field)
          case Item.demonstratedKnowledge => toSearchFieldObj(searchFields,field,dbkey=Item.otherAlignments+"."+Alignments.Keys.demonstratedKnowledge)
          case Item.originId => toSearchFieldObj(searchFields,field)
          case Item.collectionId => toSearchFieldObj(searchFields,field)
          case Item.contentType => toSearchFieldObj(searchFields,field)
          case Item.pValue => toSearchFieldObj(searchFields,field)
          case Item.relatedCurriculum => toSearchFieldObj(searchFields,field,dbkey=Item.otherAlignments+"."+Alignments.Keys.relatedCurriculum)
          case Item.gradeLevel => toSearchFieldObj(searchFields,field,dbkey=Item.taskInfo+"."+TaskInfo.Keys.gradeLevel)
          case Item.itemType => toSearchFieldObj(searchFields,field,dbkey=Item.taskInfo+"."+TaskInfo.Keys.itemType)
          case Item.keySkills => toSearchFieldObj(searchFields,field,dbkey=Item.otherAlignments+"."+Alignments.Keys.keySkills)
          case Item.bloomsTaxonomy => toSearchFieldObj(searchFields,field,dbkey=Item.otherAlignments+"."+Alignments.Keys.bloomsTaxonomy)
          case Item.primarySubject => toSearchFieldObj(searchFields,field,dbkey=Item.taskInfo+"."+TaskInfo.Keys.subjects+"."+Subjects.Keys.primary)
          case key if key.startsWith(Item.primarySubject) => toSearchFieldObj(searchFields,field,false)
          case Item.relatedSubject => toSearchFieldObj(searchFields,field,dbkey=Item.subjects+"."+Subjects.Keys.related)
          case key if key.startsWith(Item.relatedSubject) => toSearchFieldObj(searchFields,field,false)
          case Item.priorUse => toSearchFieldObj(searchFields,field)
          case Item.priorGradeLevel => toSearchFieldObj(searchFields,field)
          case Item.reviewsPassed => toSearchFieldObj(searchFields,field)
          case Item.standards => toSearchFieldObj(searchFields,field)
          case key if key.startsWith(Item.standards) => toSearchFieldObj(searchFields,field,false)
          case Item.title => toSearchFieldObj(searchFields,field,dbkey=Item.taskInfo+"."+TaskInfo.Keys.title)
          case Item.published => toSearchFieldObj(searchFields,field)
          case _ => Left(InternalError("unknown key contained in fields: "+field._1,addMessageToClientOutput = true))
        }
      }
      case Left(e) => Left(e)
    })
  }

  private def preParseSubjects(dbquery:BasicDBObject)(implicit parseFields:Map[String,(AnyRef) => Either[InternalError,AnyRef]]):Either[SearchCancelled,MongoDBObject] = {
    val primarySubjectQuery = dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == Item.primarySubject+"."+Subject.Subject =>
            formatQuery(Subject.Subject,field._2,searchobj)
          case key if key == Item.primarySubject+"."+Subject.Category =>
            formatQuery(Subject.Category,field._2,searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty){
        val subjects = Utils.toSeq(Subject.find(searchobj)).map(_.id)
        if (subjects.size > 1) Right(MongoDBObject(Item.taskInfo+"."+TaskInfo.Keys.subjects+"."+Subjects.Keys.primary -> MongoDBObject("$in" -> subjects)))
        else if (subjects.size == 1) Right(MongoDBObject(Item.taskInfo+"."+TaskInfo.Keys.subjects+"."+Subjects.Keys.primary -> subjects.head))
        else Left(SearchCancelled(None))
      } else Right(MongoDBObject())
      case Left(sc) => Left(sc)
    }
    val relatedSubjectQuery = dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == Item.relatedSubject+"."+Subject.Subject =>
            formatQuery(Subject.Subject,field._2,searchobj)
          case key if key == Item.relatedSubject+"."+Subject.Category =>
            formatQuery(Subject.Category,field._2,searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty){
        val subjects:Seq[ObjectId] = Utils.toSeq(Subject.find(searchobj)).map(_.id)
        if (subjects.size > 1) Right(MongoDBObject(Item.taskInfo+"."+TaskInfo.Keys.subjects+"."+Subjects.Keys.related -> MongoDBObject("$in" -> subjects)))
        else if (subjects.size == 1) Right(MongoDBObject(Item.taskInfo+"."+TaskInfo.Keys.subjects+"."+Subjects.Keys.related -> subjects.head))
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
  private def preParseStandards(dbquery:BasicDBObject)(implicit parseFields:Map[String,(AnyRef) => Either[InternalError,AnyRef]]):Either[SearchCancelled,MongoDBObject] = {
    dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
      result match {
        case Right(searchobj) => field._1 match {
          case key if key == Item.standards+"."+Standard.DotNotation => formatQuery(Standard.DotNotation,field._2,searchobj)
          case key if key == Item.standards+"."+Standard.guid =>formatQuery(Standard.guid,field._2,searchobj)
          case key if key == Item.standards+"."+Standard.Subject =>formatQuery(Standard.Subject,field._2,searchobj)
          case key if key == Item.standards+"."+Standard.Category => formatQuery(Standard.Category,field._2,searchobj)
          case key if key == Item.standards+"."+Standard.SubCategory => formatQuery(Standard.SubCategory,field._2,searchobj)
          case _ => Right(searchobj)
        }
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => if (searchobj.nonEmpty){
        val standards:Seq[String] = Utils.toSeq(Standard.find(searchobj)).map(_.dotNotation).flatten
        if (standards.nonEmpty) {
          if(standards.size == 1) Right(MongoDBObject(Item.standards -> standards.head))
          else Right(MongoDBObject(Item.standards -> MongoDBObject("$in" -> standards)))
        }else Left(SearchCancelled(None))
      }else Right(MongoDBObject())
      case Left(sc) => Left(sc)
    }
  }
  override protected def toSearchObjInternal(dbquery:BasicDBObject, optInitSearch:Option[MongoDBObject])(implicit parseFields:Map[String,(AnyRef) => Either[InternalError,AnyRef]]):Either[SearchCancelled,MongoDBObject] = {
    preParseStandards(dbquery) match {
      case Right(query1) => preParseSubjects(dbquery) match {
        case Right(query2) => {
          val initSearch = query1 ++ query2.asDBObject ++ optInitSearch.getOrElse[MongoDBObject](MongoDBObject()).asDBObject
          dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(initSearch))((field,result) => result match {
            case Right(searchobj) => {
              field._1 match {
                case key if key == Item.workflow+"."+Workflow.setup => formatQuery(key,field._2,searchobj)
                case key if key == Item.workflow+"."+Workflow.tagged => formatQuery(key,field._2,searchobj)
                case key if key == Item.workflow+"."+Workflow.standardsAligned => formatQuery(key,field._2,searchobj)
                case key if key == Item.workflow+"."+Workflow.qaReview => formatQuery(key,field._2,searchobj)
                case Item.author => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.author,field._2,searchobj)
                case Item.contributor => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.contributor,field._2,searchobj)
                case Item.costForResource => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.costForResource,field._2,searchobj)
                case Item.credentials => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.credentials,field._2,searchobj)
                case Item.licenseType => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.licenseType,field._2,searchobj)
                case Item.sourceUrl => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.sourceUrl,field._2,searchobj)
                case Item.copyrightOwner => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.owner,field._2,searchobj)
                case Item.copyrightYear => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.year,field._2,searchobj)
                case Item.copyrightExpirationDate => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.expirationDate,field._2,searchobj)
                case Item.copyrightImageName => formatQuery(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.imageName,field._2,searchobj)
                case Item.lexile => formatQuery(Item.lexile,field._2,searchobj)
                case Item.demonstratedKnowledge => formatQuery(Item.otherAlignments+"."+Alignments.Keys.demonstratedKnowledge,field._2,searchobj)
                case Item.originId => formatQuery(Item.originId,field._2,searchobj)
                case Item.collectionId => formatQuery(Item.collectionId,field._2,searchobj)
                case Item.published => formatQuery(Item.published, field._2, searchobj)
                case Item.contentType => Right(searchobj)
                case Item.pValue => formatQuery(Item.pValue,field._2,searchobj)
                case Item.relatedCurriculum => formatQuery(Item.otherAlignments+"."+Alignments.Keys.relatedCurriculum,field._2,searchobj)
                case Item.supportingMaterials => Left(SearchCancelled(Some(InternalError("cannot query on supportingMaterials",addMessageToClientOutput = true))))
                case Item.gradeLevel => formatQuery(Item.taskInfo+"."+TaskInfo.Keys.gradeLevel,field._2,searchobj)
                case Item.itemType => formatQuery(Item.taskInfo+"."+TaskInfo.Keys.itemType,field._2,searchobj)
                case Item.keySkills => formatQuery(Item.otherAlignments+"."+Alignments.Keys.keySkills,field._2,searchobj)
                case Item.bloomsTaxonomy => formatQuery(Item.otherAlignments+"."+Alignments.Keys.bloomsTaxonomy,field._2,searchobj)
                case key if key.startsWith(Item.primarySubject) => Right(searchobj)
                case key if key == key.startsWith(Item.relatedSubject) => Right(searchobj)
                case Item.priorUse => formatQuery(Item.priorUse,field._2,searchobj)
                case Item.priorGradeLevel => formatQuery(Item.priorGradeLevel,field._2,searchobj)
                case Item.reviewsPassed => formatQuery(Item.reviewsPassed,field._2,searchobj)
                case key if key.startsWith(Item.standards) => Right(searchobj)
                case Item.title => formatQuery(Item.taskInfo+"."+TaskInfo.Keys.title,field._2,searchobj)
                case _ => Left(SearchCancelled(Some(InternalError("unknown key contained in query: "+field._1,addMessageToClientOutput = true))))
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

  override protected def toSortObjInternal(field:(String,AnyRef)):Either[InternalError,MongoDBObject] = {
    def formatSortField(key:String,value:AnyRef):Either[InternalError,MongoDBObject] = {
      value match {
        case intval:java.lang.Integer => Right(MongoDBObject(key -> value))
        case _ => Left(InternalError("sort value not a number",addMessageToClientOutput = true))
      }
    }
    field._1 match {
      case key if key == Item.workflow+"."+Workflow.setup => formatSortField(key,field._2)
      case key if key == Item.workflow+"."+Workflow.tagged => formatSortField(key,field._2)
      case key if key == Item.workflow+"."+Workflow.standardsAligned => formatSortField(key,field._2)
      case key if key == Item.workflow+"."+Workflow.qaReview => formatSortField(key,field._2)
      case Item.author => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.author,field._2)
      case Item.contributor => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.contributor,field._2)
      case Item.costForResource => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.costForResource,field._2)
      case Item.credentials => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.credentials,field._2)
      case Item.licenseType => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.licenseType,field._2)
      case Item.sourceUrl => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.sourceUrl,field._2)
      case Item.copyrightOwner => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.owner,field._2)
      case Item.copyrightYear => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.year,field._2)
      case Item.copyrightExpirationDate => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.expirationDate,field._2)
      case Item.copyrightImageName => formatSortField(Item.contributorDetails+"."+ContributorDetails.Keys.copyright+"."+Copyright.Keys.imageName,field._2)
      case Item.lexile => formatSortField(Item.lexile,field._2)
      case Item.demonstratedKnowledge => formatSortField(Item.otherAlignments+"."+Alignments.Keys.demonstratedKnowledge,field._2)
      case Item.originId => formatSortField(Item.originId,field._2)
      case Item.contentType => formatSortField(Item.contentType,field._2)
      case Item.pValue => formatSortField(Item.pValue,field._2)
      case Item.relatedCurriculum => formatSortField(Item.otherAlignments+"."+Alignments.Keys.relatedCurriculum,field._2)
      case Item.gradeLevel => formatSortField(Item.taskInfo+"."+TaskInfo.Keys.gradeLevel,field._2)
      case Item.itemType => formatSortField(Item.taskInfo+"."+TaskInfo.Keys.itemType,field._2)
      case Item.keySkills => formatSortField(Item.otherAlignments+"."+Alignments.Keys.keySkills,field._2)
      case Item.bloomsTaxonomy => formatSortField(Item.otherAlignments+"."+Alignments.Keys.bloomsTaxonomy,field._2)
      case Item.priorUse => formatSortField(Item.priorUse,field._2)
      case Item.priorGradeLevel => formatSortField(Item.priorGradeLevel,field._2)
      case Item.reviewsPassed => formatSortField(Item.reviewsPassed,field._2)
      case key if key == Item.standards+"."+Standard.DotNotation => formatSortField(Item.standards,field._2)
      case Item.title => formatSortField(Item.taskInfo+"."+TaskInfo.Keys.title,field._2)
      case Item.collectionId => formatSortField(Item.collectionId,field._2)
      case Item.published => formatSortField(Item.published,field._2)
      case _ => Left(InternalError("unknown or invalid key contained in sort field",addMessageToClientOutput = true))
    }
  }

}
