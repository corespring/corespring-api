package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.errors.{CollectionAuthorizationError, PlatformServiceError}
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.VirtualFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.v2.auth.PassageAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{incorrectJsonFormat, noJson}
import org.corespring.v2.errors.V2Error
import org.corespring.passage.search._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import scalaz.{Failure, Success, Validation}

class PassageApi(
  passageAuth: PassageAuth,
  passageIndexService: PassageIndexService,
  orgCollectionService: OrgCollectionService,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def get(passageId: VersionedId[ObjectId], itemId: Option[String]) = futureWithIdentity { (identity, request) =>
    Future.successful(passageAuth.loadForRead(passageId.toString, itemId.map(VersionedId(_)).flatten)(identity) match {
      case Success(passage) => PassageResponseWriter.write(passage)
      case Failure(error) => Status(error.statusCode)(error.message)
    })
  }

  def search = futureWithIdentity { (identity, request) =>
    implicit val ApiReads = PassageIndexQuery.ApiReads
    implicit val PassageIndexSearchResultFormat = PassageIndexSearchResult.Format

    request.body.asJson match {
      case Some(json) => Json.fromJson[PassageIndexQuery](json) match {
        case JsSuccess(query, _) => {
          passageIndexService.search(query.scopedTo(identity)).map(_ match {
            case Success(result) => Ok(Json.prettyPrint(Json.toJson(result)))
            case _ => {
              val error = incorrectJsonFormat(json)
              Status(error.statusCode)(error.message)
            }
          })
        }
        case _ => {
          val error = incorrectJsonFormat(json)
          Future.successful(Status(error.statusCode)(error.message))
        }
      }
      case _ => {
        val error = noJson
        Future.successful(Status(error.statusCode)(error.message))
      }
    }
  }

  def create() = futureWithIdentity { (identity, request) =>
    implicit val Format = Passage.Format

    getCollectionId(identity, request) match {
      case Success(collectionId) => {
        val passage = Passage(collectionId = collectionId)
        passageAuth.insert(passage)(identity, ec).map(_ match {
          case Success(passage) => Created(Json.prettyPrint(Json.toJson(passage)))
          case Failure(error) => Status(error.statusCode)(error.message)
        })
      }
      case Failure(error) => {
        println(error.getClass)
        Future.successful(error match {
          case CollectionAuthorizationError(_, _, _*) => Status(UNAUTHORIZED)(error.message)
          case _ => Status(INTERNAL_SERVER_ERROR)(error.message)
        })
      }
    }
  }

  private def getCollectionId(identity: OrgAndOpts, request: Request[AnyContent]): Validation[PlatformServiceError, String] = {
    request.body.asJson match {
      case Some(json) => (json \ "collectionId").asOpt[String] match {
        case Some(collectionId) =>
          orgCollectionService.isAuthorized(identity.org.id, new ObjectId(collectionId), Permission.Write) match {
            case true => Success(collectionId)
            case _ =>
              Failure(CollectionAuthorizationError(identity.org.id, Permission.Write, new ObjectId(collectionId)))
          }
        case _ => orgCollectionService.getDefaultCollection(identity.org.id).map(_.id.toString)
      }
      case _ => orgCollectionService.getDefaultCollection(identity.org.id).map(_.id.toString)
    }
  }

  private object PassageResponseWriter {

    def write(passage: Passage): SimpleResult = passage.file match {
      case file: VirtualFile => Ok(file.content).withHeaders("Content-Type" -> file.contentType)
      case _ => throw new UnsupportedOperationException("Cannot support files other than VirtualFile")
    }

  }

}
