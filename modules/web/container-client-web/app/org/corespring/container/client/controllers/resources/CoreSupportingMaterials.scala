package org.corespring.container.client.controllers.resources

import java.io.FileInputStream

import org.apache.commons.io.IOUtils
import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.controllers.resources.CoreSupportingMaterials.Errors
import org.corespring.container.client.hooks._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.libs.{ Files, MimeTypes }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

private[resources] object CoreSupportingMaterials {

  case class Error(code: Int, msg: String) {
    def result = play.api.mvc.Results.Status(code)(json)
    def json = Json.obj("error" -> msg)
  }

  object Errors {
    import play.api.http.Status._
    val notMultipartForm = Error(BAD_REQUEST, "Request body can't be read  as multipart form data")
    val notJson = Error(BAD_REQUEST, "Request body can't be read as json")
    val notText = Error(BAD_REQUEST, "Request body can't be read as text")
    def cantFindParameter(name: String) = Error(BAD_REQUEST, s"Can't find parameter $name")
    def mimeTypeNotAcceptable(mimeType: String, types: Seq[String]) = Error(BAD_REQUEST, s"mimeType: $mimeType not acceptable. must be one of the following mimeTypes: ${types.mkString(",")}")
    def cantDetectMimeType(name: String, contentType: Option[String]) = Error(BAD_REQUEST, s"can't detect mimeType for file: $name, contentType set to: ${contentType.getOrElse("nothing")}")
    def throwableError(t: Throwable) = Error(INTERNAL_SERVER_ERROR, s"A server side error occured: ${t.getMessage}")
  }
}

private[resources] trait CoreSupportingMaterials extends Controller with HasContainerContext {

  type E = CoreSupportingMaterials.Error
  import Errors._

  private val logger = Logger(classOf[CoreSupportingMaterials])

  def materialHooks: SupportingMaterialHooks

  val acceptableTypes = Seq(
    "application/pdf",
    "image/png",
    "image/gif",
    "image/jpeg")

  private def accept(types: Seq[String])(mimeType: String) = {
    if (types.contains(mimeType)) {
      Success(true)
    } else {
      Failure(mimeTypeNotAcceptable(mimeType, types))
    }
  }

  def createSupportingMaterialFromFile(id: String) = Action.async { request =>

    def createFromMultipartForm(form: MultipartFormData[Files.TemporaryFile]): Validation[E, CreateBinaryMaterial] = {
      for {
        fileAndBinary <- formToBinary(form, acceptableTypes)
        filename <- Success(fileAndBinary._1)
        binary <- Success(fileAndBinary._2)
        name <- Success(form.asFormUrlEncoded.get("name").map(_.mkString).getOrElse(filename))
        materialType <- form.asFormUrlEncoded.get("materialType").map(_.mkString).toSuccess(cantFindParameter("materialType"))
      } yield {
        CreateBinaryMaterial(name, materialType, binary)
      }
    }

    create((body) => for {
      multipart <- request.body.asMultipartFormData.toSuccess(notMultipartForm)
      sm <- createFromMultipartForm(multipart)
    } yield sm)(id, request)
  }

  private def create[F <: File](mk: AnyContent => Validation[E, CreateNewMaterialRequest[F]])(id: String, r: Request[AnyContent]): Future[SimpleResult] = {
    mk(r.body) match {
      case Success(sm) => {
        val f: Future[Either[(Int, String), JsValue]] = materialHooks.create(id, sm)(r)
        f.map { e =>
          e match {
            case Left((err, msg)) => Status(err)(msg)
            case Right(json) => Status(CREATED)(json)
          }
        }
      }
      case Failure(e) => Future(e.result)
    }
  }

  def createSupportingMaterial(id: String) = Action.async { request =>

    def createFromJson(json: JsValue): Validation[E, CreateHtmlMaterial] = {
      for {
        name <- (json \ "name").asOpt[String].toSuccess(cantFindParameter("name"))
        materialType <- (json \ "materialType").asOpt[String].toSuccess(cantFindParameter("materialType"))
        html <- (json \ "html").asOpt[String].toSuccess(cantFindParameter("html"))
      } yield {
        CreateHtmlMaterial(
          name,
          materialType,
          Html("index.html", html),
          Seq.empty)
      }
    }

    create((body) => for {
      json <- body.asJson.toSuccess(notJson)
      sm <- createFromJson(json)
    } yield sm)(id, request)
  }

  private implicit class ToR(in: Hooks.R[JsValue]) {
    def toResult: Future[SimpleResult] = {
      in.map { e =>
        e match {
          case Left((code, msg)) => Status(code)(msg)
          case Right(json) => Ok(json)
        }
      }
    }
  }

  def deleteSupportingMaterial(id: String, name: String) = Action.async { implicit request =>
    materialHooks.delete(id, name).toResult
  }

  def getTimestamp: String = DateTime.now.getMillis.toString

  private def formToBinary(form: MultipartFormData[Files.TemporaryFile], types: Seq[String]): Validation[E, (String, Binary)] = {
    for {
      file <- form.file("file").toSuccess(cantFindParameter("file"))
      mimeType <- file.contentType.orElse(MimeTypes.forFileName(file.filename)).toSuccess(cantDetectMimeType(file.filename, file.contentType))
      acceptable <- accept(types)(mimeType)
      stream <- Validation.fromTryCatch(new FileInputStream(file.ref.file)).leftMap { t =>
        if (logger.isDebugEnabled) {
          t.printStackTrace
        }
        throwableError(t)
      }
    } yield {
      val bytes = IOUtils.toByteArray(stream)
      IOUtils.closeQuietly(stream)
      val filename = s"${getTimestamp}-${file.filename}"

      logger.debug(s"function=formToBinary, filename=$filename, mimeType=$mimeType")

      file.filename -> Binary(filename, mimeType, bytes)
    }
  }

  def updateSupportingMaterialContent(id: String, materialName: String, filename: String) = Action.async { implicit request =>
    request.body.asText.map { content =>
      materialHooks.updateContent(id, materialName, filename, content).toResult
    }.getOrElse(Future(notText.result))
  }

  def addAssetToSupportingMaterial(id: String, name: String) = Action.async { implicit request =>
    val v = for {
      form <- request.body.asMultipartFormData.toSuccess(notMultipartForm)
      binary <- formToBinary(form, acceptableTypes.filterNot(_ == "application/pdf"))
    } yield materialHooks.addAsset(id, name, binary._2).map { e =>
      e.map { ur =>
        Json.obj("path" -> ur.path)
      }
    }.toResult

    v match {
      case Failure(e) => Future(e.result)
      case Success(r) => r
    }
  }

  def deleteAssetFromSupportingMaterial(id: String, name: String, filename: String) = Action.async { implicit request =>
    materialHooks.deleteAsset(id, name, filename).toResult
  }

  def getAssetFromSupportingMaterial(id: String, name: String, filename: String) = Action.async { implicit request =>

    materialHooks.getAsset(id, name, filename).map { e =>
      e match {
        case Left((code, msg)) => Status(code)(msg)
        case Right(fd) => {
          val objContent: Enumerator[Array[Byte]] = Enumerator.fromStream(fd.stream)
          val headers = {
            val main = Map(
              CONTENT_LENGTH.toString -> fd.contentLength.toString,
              CONTENT_DISPOSITION.toString -> "inline")

            val contentType = fd.contentType match {
              case null => Map()
              case "application/octet-stream" => Map()
              case _ => Map(CONTENT_TYPE -> fd.contentType)
            }
            main ++ contentType ++ fd.metadata.get(ETAG).map { ETAG -> _ }
          }
          SimpleResult(header = ResponseHeader(200, headers), body = objContent)
        }
      }
    }
  }
}
