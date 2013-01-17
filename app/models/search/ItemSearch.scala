package models.search

import controllers.{LogType}
import com.mongodb.casbah.Imports._
import java.util.regex.Pattern
import models._
import com.mongodb.util.{JSONParseException, JSON}
import controllers.InternalError
import scala.Left
import search.SearchCancelled
import scala.Right
import scala.Some

object ItemSearch extends Searchable{
  def toFieldsObj(fields:AnyRef):Either[InternalError,SearchFields] = {
    fields match {
      case strfields:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strfields).asInstanceOf[BasicDBObject]
        toFieldsObj(parsedobj)
      }catch {
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))
      }
      case dbfields:BasicDBObject => {
        var method:Int = if(dbfields.valuesIterator.next() == 1) 1 else 0
        def toSearchFieldObj(fieldsObj:MongoDBObject,field:(String,AnyRef),addToFieldsObj:Boolean = true)(implicit searchFields:SearchFields):Either[InternalError,SearchFields] = {

        }
        dbfields.foldRight[Either[InternalError,SearchFields]](Right(SearchFields()))((field,result) => result match {
          case Right(searchFields) => field._1 match {
            case Item.workflow => if (field._2 == method) Right( += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.author => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.contributor => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.costForResource => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.credentials => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.licenseType => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.sourceUrl => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.copyrightOwner => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.copyrightYear => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.copyrightExpirationDate => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.copyrightImageName => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.lexile => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.demonstratedKnowledge => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.originId => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.collectionId => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.contentType => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.pValue => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.relatedCurriculum => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.gradeLevel => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.itemType => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.keySkills => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
            case Item.primarySubject => if (field._2 == method) Right(fieldsobj += field) else Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))

          }
          case Left(e) => Left(e)
        })
      }
    }
  }
  def toSearchObj(query: AnyRef, optInitSearch:Option[MongoDBObject]):Either[SearchCancelled,MongoDBObject] = {
    def formatStringQuery(key:String, value:AnyRef, searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case strval:String => Right(searchobj += key -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
        case _ => Left(SearchCancelled(Some(InternalError("invalid value when parsing search for "+key))))
      }
    }
    def formatBooleanQuery(key:String,value:AnyRef,searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case boolval:Boolean => Right(searchobj += key -> value)
        case _ => Left(SearchCancelled(Some(InternalError(key+" did not have value of type boolean",addMessageToClientOutput = true))))
      }
    }
    def formatStringQuerySubjects(key:String,subjectsKey:String,value:AnyRef,searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case strval:String => {
          val ids = Subject.find(MongoDBObject(subjectsKey -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))).map(_.id)
          if (ids.nonEmpty) Right(searchobj += key -> MongoDBObject("$in" -> ids))
          else Left(SearchCancelled(None))
        }
        case _ => Left(SearchCancelled(Some(InternalError(key+" did not have value of type string",addMessageToClientOutput = true))))
      }
    }
    def formatStringQueryStandards(standardsKey:String, value:AnyRef,searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case strval:String => {
          val standards = Standard.find(MongoDBObject(standardsKey -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))).map(_.dotNotation)
          if (standards.nonEmpty) Right(searchobj += Item.standards -> MongoDBObject("$in" -> standards))
          else Left(SearchCancelled(None))
        }
        case _ => Left(SearchCancelled(Some(InternalError(Item.standards+"."+standardsKey+" did not have value of type string",addMessageToClientOutput = true))))
      }
    }
    query match {
      case strquery:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strquery).asInstanceOf[BasicDBObject]
        toSearchObj(parsedobj,optInitSearch)
      }catch {
        case e:JSONParseException => Left(SearchCancelled(Some(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))))
      }
      case dbquery:BasicDBObject => {
        dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => result match {
          case Right(searchobj) => field._1 match {
            case key if key == Item.workflow+"."+Workflow.setup => formatBooleanQuery(key,field._2,searchobj)
            case key if key == Item.workflow+"."+Workflow.tagged => formatBooleanQuery(key,field._2,searchobj)
            case key if key == Item.workflow+"."+Workflow.standardsAligned => formatBooleanQuery(key,field._2,searchobj)
            case key if key == Item.workflow+"."+Workflow.qaReview => formatBooleanQuery(key,field._2,searchobj)
            case Item.author => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.author,field._2,searchobj)
            case Item.contributor => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.contributor,field._2,searchobj)
            case Item.costForResource => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.costForResource,field._2,searchobj)
            case Item.credentials => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.credentials,field._2,searchobj)
            case Item.licenseType => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.licenseType,field._2,searchobj)
            case Item.sourceUrl => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.sourceUrl,field._2,searchobj)
            case Item.copyrightOwner => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.owner,field._2,searchobj)
            case Item.copyrightYear => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.year,field._2,searchobj)
            case Item.copyrightExpirationDate => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.expirationDate,field._2,searchobj)
            case Item.copyrightImageName => formatStringQuery(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.imageName,field._2,searchobj)
            case Item.lexile => formatStringQuery(Item.lexile,field._2,searchobj)
            case Item.demonstratedKnowledge => formatStringQuery(Item.demonstratedKnowledge,field._2,searchobj)
            case Item.originId => formatStringQuery(Item.originId,field._2,searchobj)
            case Item.collectionId => Left(SearchCancelled(Some(InternalError("cannot query on collections",addMessageToClientOutput = true))))
            case Item.contentType => Right(searchobj)
            case Item.pValue => formatStringQuery(Item.pValue,field._2,searchobj)
            case Item.relatedCurriculum => formatStringQuery(Item.relatedCurriculum,field._2,searchobj)
            case Item.supportingMaterials => Left(SearchCancelled(Some(InternalError("cannot query on supportingMaterials",addMessageToClientOutput = true))))
            case Item.gradeLevel => formatStringQuery(Item.gradeLevel,field._2,searchobj)
            case Item.itemType => formatStringQuery(Item.itemType,field._2,searchobj)
            case Item.keySkills => formatStringQuery(Item.keySkills,field._2,searchobj)
            case key if key == Item.primarySubject+"."+Subject.Subject =>
              formatStringQuerySubjects(Item.subjects+"."+Subjects.primary,Subject.Subject,field._2,searchobj)
            case key if key == Item.primarySubject+"."+Subject.Category =>
              formatStringQuerySubjects(Item.subjects+"."+Subjects.primary,Subject.Category,field._2,searchobj)
            case key if key == Item.relatedSubject+"."+Subject.Subject =>
              formatStringQuerySubjects(Item.subjects+"."+Subjects.related,Subject.Subject,field._2,searchobj)
            case key if key == Item.relatedSubject+"."+Subject.Category =>
              formatStringQuerySubjects(Item.subjects+"."+Subjects.related,Subject.Category,field._2,searchobj)
            case Item.priorUse => formatStringQuery(Item.priorUse,field._2,searchobj)
            case Item.priorGradeLevel => formatStringQuery(Item.priorGradeLevel,field._2,searchobj)
            case Item.reviewsPassed => formatStringQuery(Item.reviewsPassed,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.DotNotation => formatStringQueryStandards(Standard.DotNotation,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.guid =>formatStringQueryStandards(Standard.guid,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.Subject =>formatStringQueryStandards(Standard.Subject,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.Category => formatStringQueryStandards(Standard.Category,field._2,searchobj)
            case _ => Left(SearchCancelled(Some(InternalError("unknown key contained in query",addMessageToClientOutput = true))))
          }
          case Left(e) => Left(e)
        }) match {
          case Right(searchobj) => optInitSearch match {
            case Some(initSearch) => Right(searchobj ++ initSearch.asDBObject)
            case None => Right(searchobj)
          }
          case Left(e) => Left(e)
        }
      }
      case _ => Left(SearchCancelled(Some(InternalError("invalid search object",LogType.printFatal,addMessageToClientOutput = true))))
    }
  }
}
