package api.v1

import play.api.mvc.{Result, Request, BodyParser, Action}
import controllers.auth.BaseApi
import models.{Resource, StoredFile, Item}
import org.bson.types.ObjectId
import controllers.S3Service
import play.api.Logger
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json._
import api.ApiError
import play.api.libs.json.Json
import com.sun.jndi.dns.ResourceRecord

object ResourceApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  private val USE_ITEM_DATA_KEY: String = "__!data!__"

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
                action(request)
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
      Action(S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/resource/" + filename)) {
        request =>

          val item = Item.findOneById(new ObjectId(itemId)).get
          val resource = item.data.get

          val file = new StoredFile(
            filename,
            contentType(filename),
            false,
            itemId + "/resource/" + filename)

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
          val item = Item.findOneById(new ObjectId(itemId)).get
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
      val item = Item.findOneById(new ObjectId(itemId)).get
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
                val item = Item.findOneById(new ObjectId(itemId)).get
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
        val item : Item = Item.findOneById(new ObjectId(itemId)).get
        item.supportingMaterials = item.supportingMaterials.filter( _.name != resourceName )
        Item.save(item)
        Ok(toJson(item.supportingMaterials))
    }
  )

  val SuffixToContentTypes = Map(
    "jpg" -> "image/jpg",
    "jpeg" -> "image/jpg",
    "png" -> "image/png",
    "gif" -> "image/gif",
    "doc" -> "application/msword",
    "pdf" -> "application/pdf")

  private def storageKey(itemId: String, materialName: String, filename: String) = itemId + "/materials/" + materialName + "/" + filename

  private def contentType(filename: String): String = {
    val split = filename.split("\\.").toList
    val suffix = split.last
    SuffixToContentTypes.getOrElse(suffix, "unknown")
  }

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
