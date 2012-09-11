package api.v1

import play.api.mvc._
import controllers.auth.BaseApi
import models._
import org.bson.types.ObjectId
import controllers.S3Service
import play.api.Logger
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json._
import api.ApiError
import play.api.libs.json.Json
import com.sun.jndi.dns.ResourceRecord
import scala.Some
import scala.Some

object ResourceApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  private val USE_ITEM_DATA_KEY: String = "__!data!__"

  val DATA_PATH: String = "data"

  /**
   * A class that adds an AuthorizationContext to the Request object
   * @param item - the found item
   * @param r - the Request
   * @tparam A - the type determining the type of the body parser (eg: AnyContent)
   */
  case class ItemRequest[A](item: Item, r: Request[A]) extends WrappedRequest(r)

  /**
   * A wrapping Action that checks that an Item with the given id exists.
   * @param itemId - the item id
   * @param additionalChecks - a sequence of additional checks beyond checking for the item.
   * @param action - the action to invoke
   * @tparam A
   */
  case class HasItem[A](
                         itemId: String,
                         additionalChecks: Seq[Item => Option[ApiError]] = Seq(),
                         action: Action[A]) extends Action[A] {

    def apply(request: Request[A]): Result = {
      val id = objectId(itemId)

      id match {
        case Some(validId) => {
          Item.findOneById(new ObjectId(itemId)) match {
            case Some(item) => {

              val errors: Seq[ApiError] = additionalChecks.flatMap(_(item))

              if (errors.length == 0) {
                action(ItemRequest(item, request))
                //action(request)
              }
              else {
                //TODO: Only returning the first error
                NotFound(toJson(errors(0)))
              }
            }
            case _ => NotFound
          }
        }
        case _ => NotFound
      }
    }

    private def objectId(itemId: String): Option[ObjectId] = {
      try {
        Some(new ObjectId(itemId))
      }
      catch {
        case e: Exception => None
      }
    }

    lazy val parser = action.parser
  }


  def createSupportingMaterialFile(itemId: String, resourceName: String) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        request.body.asJson match {
          case Some(json) => {
            val item = request.asInstanceOf[ItemRequest[AnyContent]].item
            item.supportingMaterials.find(_.name == resourceName) match {
              case Some(r) => {
                json.asOpt[BaseFile] match {
                  case Some(file) => {
                    saveFileIfNameNotTaken(item, r, file)
                  }
                  case _ => BadRequest
                }
              }
              case _ => NotFound
            }
          }
          case _ => BadRequest
        }
    }
  )


  def createDataFile(itemId: String) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        request.body.asJson match {
          case Some(json) => {
            val item = request.asInstanceOf[ItemRequest[AnyContent]].item
            json.asOpt[BaseFile] match {
              case Some(file) => {
                if (!item.data.isDefined) {
                  throw new RuntimeException("item.data should never be undefined")
                }
                saveFileIfNameNotTaken(item, item.data.get, file)
              }
              case _ => BadRequest
            }
          }
          case _ => BadRequest
        }
    }
  )

  /**
   * Upload a file to the 'data' Resource in the Item.
   * @param itemId
   * @param filename
   * @return
   */
  def uploadFileToData(itemId: String, filename: String) =
    HasItem(
      itemId,
      Seq(isFilenameTaken(filename, USE_ITEM_DATA_KEY)),
      Action(S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/" + DATA_PATH + "/" + filename)) {
        request =>
          val item = request.asInstanceOf[ItemRequest[AnyContent]].item
          val resource = item.data.get

          val file = new StoredFile(
            filename,
            contentType(filename),
            false,
            itemId + "/" + DATA_PATH + "/" + filename)

          resource.files = resource.files ++ Seq(file)
          Item.save(item)
          Ok(toJson(file))
      }
    )

  /**
   * Upload a file to a supporting material Resource in the item.
   * @param itemId
   * @param materialName
   * @param filename
   * @return
   */
  def uploadFile(itemId: String, materialName: String, filename: String) =
    HasItem(itemId,
      Seq(
        canFindResource(materialName)(_),
        isFilenameTaken(filename, materialName)(_)
      ),

      Action(S3Service.s3upload(AMAZON_ASSETS_BUCKET, storageKey(itemId, materialName, filename))) {
        request =>
          val item = request.asInstanceOf[ItemRequest[AnyContent]].item
          val resource = item.supportingMaterials.find(_.name == materialName).get

          val file = new StoredFile(
            filename,
            contentType(filename),
            false,
            storageKey(itemId, materialName, filename))
          resource.files = resource.files ++ Seq(file)
          Item.save(item)
          Ok(toJson(file))
      }
    )

  def getSupportingMaterials(itemId: String) = HasItem(itemId, Seq(), Action {
    request =>
      val item = request.asInstanceOf[ItemRequest[AnyContent]].item
      Ok(toJson(item.supportingMaterials))
  })

  def createSupportingMaterial(itemId: String) = HasItem(itemId,
    Seq(),
    Action {
      request =>
        request.body.asJson match {
          case Some(json) => {

            json.asOpt[Resource] match {
              case Some(foundResource) => {
                val item = request.asInstanceOf[ItemRequest[AnyContent]].item
                isResourceNameTaken(foundResource.name)(item) match {
                  case Some(error) => NotAcceptable(toJson(error))
                  case _ => {
                    item.supportingMaterials = item.supportingMaterials ++ Seq[Resource](foundResource)
                    Item.save(item)
                    Ok(toJson(foundResource))
                  }
                }
              }
              case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
            }
          }
          case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
        }
    })

  def deleteSupportingMaterial(itemId: String, resourceName: String) = HasItem(itemId,
    Seq(canFindResource(resourceName)(_)),
    Action {
      request =>
        val item = request.asInstanceOf[ItemRequest[AnyContent]].item
        item.supportingMaterials = item.supportingMaterials.filter(_.name != resourceName)
        Item.save(item)
        Ok("")
    }
  )

  private def unsetIsMain(resource:Resource) {
    resource.files = resource.files.map((f: BaseFile) => {
      if (f.isInstanceOf[VirtualFile]) {
        val vf = f.asInstanceOf[VirtualFile]
        VirtualFile(f.name, f.contentType, isMain = false, content = vf.content)
      }
      else {
        val sf = f.asInstanceOf[StoredFile]
        StoredFile(f.name, f.contentType, isMain = false, storageKey = sf.storageKey)
      }
    })
  }

  private def saveFileIfNameNotTaken(item: Item, resource: Resource, file: BaseFile): Result =
    resource.files.find(_.name == file.name) match {
      case Some(existingFile) => NotAcceptable(toJson(ApiError.FilenameTaken(Some(file.name))))
      case _ => {
        if (file.isMain == true) {
          unsetIsMain(resource)
        }
        resource.files = resource.files ++ Seq(file)
        Item.save(item)
        Ok(toJson(file))
      }
    }

  private def storageKey(itemId: String, materialName: String, filename: String) = itemId + "/materials/" + materialName + "/" + filename

  private def contentType(filename: String): String = BaseFile.getContentType(filename)

  /**
   * check that the item contains a supportingMaterial resource with the supplied name.
   */
  private def canFindResource(resourceName: String)(item: Item): Option[ApiError] = {
    if (item.supportingMaterials.exists(_.name == resourceName)) {
      None
    }
    else {
      Some(ApiError.ResourceNotFound(Some(resourceName)))
    }
  }

  private def getResource(item: Item, resourceName: String): Option[Resource] = {
    if (resourceName == USE_ITEM_DATA_KEY) {
      item.data
    } else {
      item.supportingMaterials.find(_.name == resourceName)
    }

  }

  private def isResourceNameTaken(resourceName: String)(item: Item): Option[ApiError] = {
    getResource(item, resourceName) match {
      case Some(r) => Some(ApiError.ResourceNameTaken(Some(resourceName)))
      case _ => None
    }
  }

  private def isFilenameTaken(filename: String, resourceName: String)(item: Item): Option[ApiError] = {

    getResource(item, resourceName) match {
      case Some(r) => {
        if (r.files.exists(_.name == filename)) {
          Some(ApiError.FilenameTaken(Some(filename)))
        }
        else {
          None
        }
      }
      case _ => None
    }
  }
}
