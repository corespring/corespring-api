package api.v1

import controllers.auth.{ ApiRequest, BaseApi }
import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.metadata.MetadataSet
import org.corespring.platform.core.services.metadata.MetadataSetServiceImpl
import org.corespring.platform.core.services.organization.OrganizationService
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsValue }
import play.api.mvc.{ AnyContent, Result }
import scala.language.implicitConversions
import scalaz.Scalaz._
import scalaz._

class MetadataSetApi(metadataSetService: MetadataSetServiceImpl) extends BaseApi {

  def list = ApiAction {
    request =>
      Ok(metadataSetService.list(request.ctx.organization).as[JsValue])
  }

  def get(id: ObjectId) = ApiAction {
    request =>
      metadataSetService.findOneById(id).map {
        m =>
          Ok(m.as[JsValue])
      }.getOrElse(NotFound)
  }

  def create = ApiAction {
    implicit request =>
      createOrUpdate(metadataSetService.create(request.ctx.organization, _))
  }

  def update(id: ObjectId) = ApiAction {
    implicit request =>
      createOrUpdate(metadataSetService.update)
  }

  private def createOrUpdate(block: MetadataSet => Either[String, MetadataSet])(implicit r: ApiRequest[AnyContent]) = {
    val result: Validation[String, Result] = for {
      json <- r.body.asJson.toSuccess("No json in body")
      set <- json.asOpt[MetadataSet].toSuccess("Can't convert to MetadataSet")
      updatedSet <- block(set)
    } yield Ok(updatedSet.as[JsValue])

    result match {
      case Success(r) => r
      case Failure(s) => BadRequest(s)
    }
  }

  def delete(id: ObjectId) = ApiAction {
    request =>
      metadataSetService.delete(request.ctx.organization, id).map {
        error =>
          BadRequest(error)
      }.getOrElse(Ok)
  }

  implicit private def seqToJsValue(l: Seq[MetadataSet]): JsValue = JsArray(l.map(toJson(_)))

  implicit private def metadataToJsValue(m: MetadataSet): JsValue = toJson(m)

  implicit private def eitherToValidation[E, R](in: Either[E, R]): Validation[E, R] = {
    in match {
      case Left(e) => Failure(e)
      case Right(r) => Success(r)
    }
  }
}

object MetadataSetApi extends MetadataSetApi(new MetadataSetServiceImpl {
  def orgService: OrganizationService = Organization
})
