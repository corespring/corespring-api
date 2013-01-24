package models.search

import controllers.{Utils, LogType, InternalError}
import com.mongodb.casbah.Imports._
import java.util.regex.Pattern
import models._
import com.mongodb.util.{JSONParseException, JSON}
import scala.Left
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
        def toSearchFieldObj(searchFields:SearchFields,field:(String,AnyRef),addToFieldsObj:Boolean = true,dbkey:String=""):Either[InternalError,SearchFields] = {
          if(field._2 == method){
            if(addToFieldsObj) {
              if(dbkey.isEmpty) field._1 else dbkey
              searchFields.dbfields = searchFields.dbfields += ((if(dbkey.isEmpty) field._1 else dbkey) -> field._2)
            }
            searchFields.jsfields = searchFields.jsfields :+ field._1
            Right(searchFields)
          }else{
            Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
          }
        }
        dbfields.foldRight[Either[InternalError,SearchFields]](Right(SearchFields(method = method)))((field,result) => result match {
          case Right(searchFields) => field._1 match {
            case key if key.startsWith(Item.workflow) => toSearchFieldObj(searchFields,field)
            case Item.author => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.author)
            case Item.contributor => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.contributor)
            case Item.costForResource => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.costForResource)
            case Item.credentials => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.credentials)
            case Item.licenseType => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.licenseType)
            case Item.sourceUrl => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.sourceUrl)
            case Item.copyrightOwner => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.owner)
            case Item.copyrightYear => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.year)
            case Item.copyrightExpirationDate => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.expirationDate)
            case Item.copyrightImageName => toSearchFieldObj(searchFields,field,dbkey=Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.imageName)
            case Item.lexile => toSearchFieldObj(searchFields,field)
            case Item.demonstratedKnowledge => toSearchFieldObj(searchFields,field)
            case Item.originId => toSearchFieldObj(searchFields,field)
            case Item.collectionId => toSearchFieldObj(searchFields,field)
            case Item.contentType => toSearchFieldObj(searchFields,field)
            case Item.pValue => toSearchFieldObj(searchFields,field)
            case Item.relatedCurriculum => toSearchFieldObj(searchFields,field)
            case Item.gradeLevel => toSearchFieldObj(searchFields,field)
            case Item.itemType => toSearchFieldObj(searchFields,field)
            case Item.keySkills => toSearchFieldObj(searchFields,field)
            case Item.primarySubject => toSearchFieldObj(searchFields,field,dbkey=Item.subjects+"."+Subjects.primary)
            case key if key.startsWith(Item.primarySubject) => toSearchFieldObj(searchFields,field,false)
            case Item.relatedSubject => toSearchFieldObj(searchFields,field,dbkey=Item.subjects+"."+Subjects.related)
            case key if key.startsWith(Item.relatedSubject) => toSearchFieldObj(searchFields,field,false)
            case Item.priorUse => toSearchFieldObj(searchFields,field)
            case Item.priorGradeLevel => toSearchFieldObj(searchFields,field)
            case Item.reviewsPassed => toSearchFieldObj(searchFields,field)
            case Item.standards => toSearchFieldObj(searchFields,field)
            case key if key.startsWith(Item.standards) => toSearchFieldObj(searchFields,field,false)
            case Item.title => toSearchFieldObj(searchFields,field)
            case _ => Left(InternalError("unknown key contained in fields",addMessageToClientOutput = true))
          }
          case Left(e) => Left(e)
        })
      }
    }
  }

  def toSearchObj(query: AnyRef, optInitSearch:Option[MongoDBObject]):Either[SearchCancelled,MongoDBObject] = toSearchObj(query,optInitSearch,0)
  private def toSearchObj(query: AnyRef, optInitSearch:Option[MongoDBObject],depth:Int):Either[SearchCancelled,MongoDBObject] = {
    def formatStringQuery(key:String, value:AnyRef, searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case strval:String => Right(searchobj += key -> value)
        case dbobj:BasicDBObject => formatSpecOp(dbobj) match {
          case Right(newvalue) => Right(searchobj += key -> newvalue)
        }
        case _ => Left(SearchCancelled(Some(InternalError("invalid value when parsing search for "+key))))
      }
    }
    def formatBooleanQuery(key:String,value:AnyRef,searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case boolval:java.lang.Boolean => Right(searchobj += key -> boolval)
        case dbobj:BasicDBObject => formatSpecOp(dbobj) match {
          case Right(newvalue) => Right(searchobj += key -> newvalue)
        }
        case _ => Left(SearchCancelled(Some(InternalError(key+" did not have value of type boolean",addMessageToClientOutput = true))))
      }
    }
    def preParseSubjects(dbquery:BasicDBObject):Either[SearchCancelled,MongoDBObject] = {
      val primarySubjectQuery = dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
        result match {
          case Right(searchobj) => field._1 match {
            case key if key == Item.primarySubject+"."+Subject.Subject =>
              formatStringQuery(Subject.Subject,field._2,searchobj)
            case key if key == Item.primarySubject+"."+Subject.Category =>
              formatStringQuery(Subject.Category,field._2,searchobj)
            case _ => Right(searchobj)
          }
          case Left(sc) => Left(sc)
        }
      }) match {
        case Right(searchobj) => if (searchobj.nonEmpty){
          val subjects = Utils.toSeq(Subject.find(searchobj)).map(_.id)
          if (subjects.nonEmpty) Right(MongoDBObject(Item.subjects+"."+Item.primarySubject -> MongoDBObject("$in" -> subjects)))
          else Left(SearchCancelled(None))
        } else Right(MongoDBObject())
        case Left(sc) => Left(sc)
      }
      val relatedSubjectQuery = dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
        result match {
          case Right(searchobj) => field._1 match {
            case key if key == Item.relatedSubject+"."+Subject.Subject =>
              formatStringQuery(Subject.Subject,field._2,searchobj)
            case key if key == Item.relatedSubject+"."+Subject.Category =>
              formatStringQuery(Subject.Category,field._2,searchobj)
            case _ => Right(searchobj)
          }
          case Left(sc) => Left(sc)
        }
      }) match {
        case Right(searchobj) => if (searchobj.nonEmpty){
          val subjects = Utils.toSeq(Subject.find(searchobj)).map(_.id)
          if (subjects.nonEmpty) Right(MongoDBObject(Item.subjects+"."+Item.relatedSubject -> MongoDBObject("$in" -> subjects)))
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
    def preParseStandards(dbquery:BasicDBObject):Either[SearchCancelled,MongoDBObject] = {
      dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
        result match {
          case Right(searchobj) => field._1 match {
            case key if key == Item.standards+"."+Standard.DotNotation => formatStringQuery(Standard.DotNotation,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.guid =>formatStringQuery(Standard.guid,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.Subject =>formatStringQuery(Standard.Subject,field._2,searchobj)
            case key if key == Item.standards+"."+Standard.Category => formatStringQuery(Standard.Category,field._2,searchobj)
            case _ => Right(searchobj)
          }
          case Left(sc) => Left(sc)
        }
      }) match {
        case Right(searchobj) => if (searchobj.nonEmpty){
          val standards = Utils.toSeq(Standard.find(searchobj)).map(_.dotNotation)
          if (standards.nonEmpty) {
            Right(searchobj += Item.standards -> MongoDBObject("$in" -> standards))
          }
          else Left(SearchCancelled(None))
        }else Right(MongoDBObject())
        case Left(sc) => Left(sc)
      }
    }
    query match {
      case strquery:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strquery).asInstanceOf[BasicDBObject]
        toSearchObj(parsedobj,optInitSearch,0)
      }catch {
        case e:JSONParseException => Left(SearchCancelled(Some(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))))
      }
      case dbquery:BasicDBObject => {
        if(depth == 0 && dbquery.contains("$or")){
          dbquery.get("$or") match {
            case dblist:BasicDBList => dblist.foldRight[Either[SearchCancelled,MongoDBList]](Right(MongoDBList()))((orcase,result) => {
              result match {
                case Right(dblist) => orcase match {
                  case dbobj:BasicDBObject => toSearchObj(dbobj,optInitSearch,1) match {
                    case Right(searchobj) => Right(dblist += searchobj)
                    case Left(sc) => Left(sc)
                  }
                  case _ => Left(SearchCancelled(Some(InternalError("element within the array of or cases was not a db object",addMessageToClientOutput = true))))
                }
                case Left(sc) => Left(sc)
              }
            }) match {
              case Right(newlist) => Right(MongoDBObject("$or" -> newlist))
              case Left(sc) => Left(sc)
            }
            case _ => Left(SearchCancelled(Some(InternalError("$or operator did not contain a list of documents for its value",addMessageToClientOutput = true))))
          }
        } else {
          preParseStandards(dbquery) match {
            case Right(query1) => preParseSubjects(dbquery) match {
              case Right(query2) => dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(query1 ++ query2.asDBObject))((field,result) => result match {
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
                  case key if key.startsWith(Item.primarySubject) => Right(searchobj)
                  case key if key == key.startsWith(Item.relatedSubject) => Right(searchobj)
                  case Item.priorUse => formatStringQuery(Item.priorUse,field._2,searchobj)
                  case Item.priorGradeLevel => formatStringQuery(Item.priorGradeLevel,field._2,searchobj)
                  case Item.reviewsPassed => formatStringQuery(Item.reviewsPassed,field._2,searchobj)
                  case key if key.startsWith(Item.standards) => Right(searchobj)
                  case Item.title => formatStringQuery(Item.title,field._2,searchobj)
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
              case Left(sc) => Left(sc)
            }
            case Left(sc) => Left(sc)
          }
        }
      }
      case _ => Left(SearchCancelled(Some(InternalError("invalid search object",LogType.printFatal,addMessageToClientOutput = true))))
    }
  }
  def toSortObj(field:AnyRef):Either[InternalError,MongoDBObject] = {
    def formatSortField(key:String,value:AnyRef):Either[InternalError,MongoDBObject] = {
      value match {
        case intval:java.lang.Integer => Right(MongoDBObject(key -> value))
        case _ => Left(InternalError("sort value not a number",addMessageToClientOutput = true))
      }
    }
    field match {
      case strfield:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strfield).asInstanceOf[BasicDBObject]
        toSortObj(parsedobj)
      }catch {
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse sort string")))
        case e:ClassCastException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse sort string")))
      }
      case dbfield:BasicDBObject => {
        if (dbfield.toSeq.size != 1){
          Left(InternalError("cannot sort on multiple fields",addMessageToClientOutput = true))
        }else{
          val field = dbfield.toSeq.head
          field._1 match {
            case key if key == Item.workflow+"."+Workflow.setup => formatSortField(key,field._2)
            case key if key == Item.workflow+"."+Workflow.tagged => formatSortField(key,field._2)
            case key if key == Item.workflow+"."+Workflow.standardsAligned => formatSortField(key,field._2)
            case key if key == Item.workflow+"."+Workflow.qaReview => formatSortField(key,field._2)
            case Item.author => formatSortField(Item.contributorDetails+"."+ContributorDetails.author,field._2)
            case Item.contributor => formatSortField(Item.contributorDetails+"."+ContributorDetails.contributor,field._2)
            case Item.costForResource => formatSortField(Item.contributorDetails+"."+ContributorDetails.costForResource,field._2)
            case Item.credentials => formatSortField(Item.contributorDetails+"."+ContributorDetails.credentials,field._2)
            case Item.licenseType => formatSortField(Item.contributorDetails+"."+ContributorDetails.licenseType,field._2)
            case Item.sourceUrl => formatSortField(Item.contributorDetails+"."+ContributorDetails.sourceUrl,field._2)
            case Item.copyrightOwner => formatSortField(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.owner,field._2)
            case Item.copyrightYear => formatSortField(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.year,field._2)
            case Item.copyrightExpirationDate => formatSortField(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.expirationDate,field._2)
            case Item.copyrightImageName => formatSortField(Item.contributorDetails+"."+ContributorDetails.copyright+"."+Copyright.imageName,field._2)
            case Item.lexile => formatSortField(Item.lexile,field._2)
            case Item.demonstratedKnowledge => formatSortField(Item.demonstratedKnowledge,field._2)
            case Item.originId => formatSortField(Item.originId,field._2)
            case Item.contentType => formatSortField(Item.contentType,field._2)
            case Item.pValue => formatSortField(Item.pValue,field._2)
            case Item.relatedCurriculum => formatSortField(Item.relatedCurriculum,field._2)
            case Item.gradeLevel => formatSortField(Item.gradeLevel,field._2)
            case Item.itemType => formatSortField(Item.itemType,field._2)
            case Item.keySkills => formatSortField(Item.keySkills,field._2)
            case Item.priorUse => formatSortField(Item.priorUse,field._2)
            case Item.priorGradeLevel => formatSortField(Item.priorGradeLevel,field._2)
            case Item.reviewsPassed => formatSortField(Item.reviewsPassed,field._2)
            case key if key == Item.standards+"."+Standard.DotNotation => formatSortField(Item.standards,field._2)
            case Item.title => formatSortField(Item.title,field._2)
            case _ => Left(InternalError("unknown or invalid key contained in sort field",addMessageToClientOutput = true))
          }
        }
      }
    }
  }
}
