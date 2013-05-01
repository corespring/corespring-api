package api.v1

import play.api.mvc._
import controllers.auth.BaseApi
import models._
import item.Item
import item.resource.{StoredFile, VirtualFile, BaseFile, Resource}
import itemSession.ItemSession
import org.bson.types.ObjectId
import controllers.{Utils, ConcreteS3Service, S3Service}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json._
import api.ApiError
import play.api.libs.json._
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import play.Logger
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsObject

class ResourceApi(s3service:S3Service) extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  private val USE_ITEM_DATA_KEY: String = "__!data!__"

  val DATA_PATH: String = "data"

  /**
   * Item.data has at least one file that is always default its name is set here.
   * TODO: move this elsewhere
   */
  private val DEFAULT_DATA_FILE_NAME: String = Resource.QtiXml

  /**
   * A class that adds an AuthorizationContext to the Request object
   * @param item - the found item
   * @param r - the Request
   * @tparam A - the type determining the type of the body parser (eg: AnyContent)
   */
  case class ItemRequest[A](item: Item, r: Request[A]) extends WrappedRequest(r)

  /**
   * TODO: This is not working as expected - needs a rewrite.
   * Because ApiAction(p) is passing the bodyparser straight through -aka- it gets run first.
   * A wrapping Action that checks that an Item with the given id exists before executing the action body.
   * @param itemId - the item id
   * @param additionalChecks - a sequence of additional checks beyond checking for the item.
   * @param action - the action to invoke
   * @tparam A
   */
  def HasItem[A](
                  itemId: String,
                  additionalChecks: Seq[Item => Option[ApiError]],
                  p: BodyParser[A])(
                  action: ItemRequest[A] => Result
                  ) = ApiAction(p) { request =>
    objectId(itemId) match {
      case Some(validId) => {
        Item.findOneById(validId) match {
          case Some(item) => {
            val errors: Seq[ApiError] = additionalChecks.flatMap(_(item))
            if (errors.length == 0) {
              action(ItemRequest(item, request))
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

  /**
   * @param itemId
   * @return an Option[ObjectId] or None if the id is invalid
   */
  private def objectId(itemId: String): Option[ObjectId] = {
    try {
      Some(new ObjectId(itemId))
    }
    catch {
      case e: Exception => None
    }
  }

  def HasItem(itemId: String,
              additionalChecks: Seq[Item => Option[ApiError]] = Seq(),
              action: ItemRequest[AnyContent] => Result): Action[AnyContent] = HasItem(itemId, additionalChecks, parse.anyContent)(action)

  private def removeFileFromResource(item: Item, resource: Resource, filename: String): Result = {
    resource.files.find(_.name == filename) match {
      case Some(f) => {
        resource.files = resource.files.filterNot(_.name == filename)
        f match {
          case StoredFile(_,_,_,key) => s3service.delete(AMAZON_ASSETS_BUCKET,key)
          case _ => //do nothing
        }
        Item.save(item)
        Ok
      }
      case _ => NotFound(filename)
    }
  }

  def deleteSupportingMaterialFile(itemId: String, resourceName: String, filename: String) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        val item = request.asInstanceOf[ItemRequest[AnyContent]].item
        item.supportingMaterials.find(_.name == resourceName) match {
          case Some(r) => {
            removeFileFromResource(item, r, filename)
          }
          case _ => NotFound(resourceName)
        }
    }
  )

  def deleteDataFile(itemId: String, filename: String) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        val item = request.asInstanceOf[ItemRequest[AnyContent]].item
        if (filename == DEFAULT_DATA_FILE_NAME) {
          BadRequest("Can't delete " + DEFAULT_DATA_FILE_NAME)
        } else {
          removeFileFromResource(item, item.data.get, filename)
        }
    }
  )

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

                val processedFile = ensureDataFileIsMainIsCorrect(file)
                saveFileIfNameNotTaken(item, item.data.get, processedFile)
              }
              case _ => BadRequest
            }
          }
          case _ => BadRequest
        }
    }
  )

  private def ensureDataFileIsMainIsCorrect(file: BaseFile): BaseFile = {
    val isMain = file.name == DEFAULT_DATA_FILE_NAME
    copyFile(file, Some(isMain))
  }

  /**
   * Copy the file and okoptionally override the isMain attribute
   * @param file
   * @param enforceIsMain
   * @return
   */
  private def copyFile(file: BaseFile, enforceIsMain: Option[Boolean]): BaseFile = {

    def copy(file: BaseFile, isMain: Boolean): BaseFile = file match {
      case VirtualFile(name,contentType,_,contents) => VirtualFile(name,contentType,isMain, contents)
      case StoredFile(name,contentType,_,key) => StoredFile(name,contentType,isMain,key)
      case _ =>  throw new RuntimeException("Unknown file type")
    }

    enforceIsMain match {
      case Some(b) => copy(file, b)
      case _ => copy(file, file.isMain)
    }
  }

  def updateDataFile(itemId: String, filename: String, force:Boolean) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        getFileFromJson(request.body) match {
          case Some(update) => {
            val item = request.asInstanceOf[ItemRequest[AnyContent]].item
            item.data.get.files.find(_.name == filename) match {
              case Some(f) => {
                val processedUpdate = ensureDataFileIsMainIsCorrect(update)
                if(update.isInstanceOf[VirtualFile]){
                  if (f.isInstanceOf[VirtualFile]){
                    val vfupdate = update.asInstanceOf[VirtualFile]
                    val vforiginal = f.asInstanceOf[VirtualFile]
                    val diff = Utils.getLevenshteinDistance(vfupdate.content,vforiginal.content)
                    val sessions = ItemSession.find(MongoDBObject(ItemSession.Keys.itemId -> item.id)).count
                    if(force || diff == 0.0 || sessions == 0){
                      item.data.get.files = item.data.get.files.map((bf) => if (bf.name == filename) processedUpdate else bf)
                      Item.save(item)
                      Ok(toJson(processedUpdate))
                    }else{
                      Forbidden(toJson(JsObject(Seq("message" ->
                        JsString("Action cancelled. You are attempting to change an item's content that contains session data. You may force the change by appending force=true to the url, but you will invalidate the corresponding session data. It is recommended that you increment the revision of the item before changing it"),
                        "flags" -> JsArray(Seq(JsString("alert_increment")))
                      ))))
                    }
                  }else BadRequest
                } else {
                  //we don't get the storage key in the request so we need to copy it across
                  if (processedUpdate.isInstanceOf[StoredFile]) {
                    processedUpdate.asInstanceOf[StoredFile].storageKey = f.asInstanceOf[StoredFile].storageKey
                  }
                  item.data.get.files = item.data.get.files.map((bf) => if (bf.name == filename) processedUpdate else bf)
                  Item.save(item)
                  Ok(toJson(processedUpdate))
                }
              }
              case _ => NotFound(update.name)
            }
          }
          case _ => BadRequest
        }
    }
  )

  private def getFileFromJson(body: AnyContent): Option[BaseFile] = {
    body.asJson match {
      case Some(json) => {
        val file = json.asOpt[BaseFile]
        file
      }
      case _ => None
    }
  }

  def updateSupportingMaterialFile(itemId: String, resourceName: String, filename: String) = HasItem(
    itemId,
    Seq(),
    Action {
      request =>
        getFileFromJson(request.body) match {
          case Some(update) => {
            val item = request.asInstanceOf[ItemRequest[AnyContent]].item
            item.supportingMaterials.find(_.name == resourceName) match {
              case Some(resource) => {
                resource.files.find(_.name == filename) match {
                  case Some(f) => {

                    //we don't get the storage key in the request so we need to copy it across
                    if (update.isInstanceOf[StoredFile]) {
                      update.asInstanceOf[StoredFile].storageKey = f.asInstanceOf[StoredFile].storageKey
                    }

                    if (update.isMain) {
                      unsetIsMain(resource)
                    }
                    resource.files = resource.files.map(bf => if (bf.name == filename) update else bf)
                    Item.save(item)
                    Ok(toJson(update))
                  }
                  case _ => NotFound
                }
              }
              case _ => NotFound(resourceName)
            }
          }
          case _ => BadRequest
        }
    }
  )

  def key(keys : String*) : String = keys.toList.mkString("/")

  /**
   * Upload a file to the 'data' Resource in the Item.
   * @param itemId
   * @param filename
   * @return
   */
  def uploadFileToData(itemId: String, filename: String) = {
    HasItem(
      itemId,
      Seq(isFilenameTaken(filename, USE_ITEM_DATA_KEY)(_)),
      s3service.s3upload(AMAZON_ASSETS_BUCKET, key(itemId, DATA_PATH, filename)))(
    {
      request =>

        def getDataResource(item:Item) : Resource = item.data match {
          case Some(r) => r
          case _ => item.data = Some(Resource(name = "data", files = Seq())); item.data.get
        }
        request.item
        val item = request.asInstanceOf[ItemRequest[AnyContent]].item

        val resource = getDataResource(item)

        val file = new StoredFile(
          filename,
          contentType(filename),
          false,
          key(itemId, DATA_PATH, filename))

        resource.files = resource.files ++ Seq(file)
        Item.save(item)
        Ok(toJson(file))
    }
    )
  }

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
      s3service.s3upload(AMAZON_ASSETS_BUCKET, storageKey(itemId, materialName, filename)))(
    {
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

  def createSupportingMaterialWithFile(itemId: String, name: String, filename: String) = {
    val s3Key = storageKey(itemId, name, filename)
    HasItem(itemId,Seq(), s3service.s3upload(AMAZON_ASSETS_BUCKET, s3Key))(
    {
      request =>
        val item = request.asInstanceOf[ItemRequest[AnyContent]].item
        item.supportingMaterials.find(_.name == name) match {
          case Some(foundResource) => NotAcceptable(toJson(ApiError.ResourceNameTaken))
          case _ => {
            val file = new StoredFile(filename, contentType(filename), true, s3Key)
            val resource = Resource(name, Seq(file))
            item.supportingMaterials = item.supportingMaterials ++ Seq(resource)
            Item.save(item)
            Ok(toJson(resource))
          }
        }
    })
  }

  def createSupportingMaterial(itemId: String) = HasItem(itemId,Seq(), Action { request  =>

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

  private def unsetIsMain(resource: Resource) {
    resource.files = resource.files.map((f: BaseFile) => {
      f match {
        case VirtualFile(n,ct,_,k) => VirtualFile(n,ct, false, k)
        case StoredFile(n,ct,_,c) => StoredFile(n,ct, false, c)
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

  private def storageKey(itemId: String, materialName: String, filename: String) = key(itemId, "materials", materialName, filename)

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

object ResourceApi extends api.v1.ResourceApi(ConcreteS3Service)