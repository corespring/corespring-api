package org.corespring.api.v2

import org.corespring.api.v2.actions.{TokenAuthenticated, V2ApiActions}
import org.corespring.api.v2.services._
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import play.api.mvc.{AnyContent, Controller}
import scala.concurrent.ExecutionContext
import org.corespring.mongo.json.services.MongoService
import org.bson.types.ObjectId
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.models.auth.{Permission, AccessTokenService}
import scala.Some
import scalaz.Validation

class Bootstrap(
                 val itemService: ItemService,
                 val v1OrgService: OrganizationService,
                 val accessTokenService: AccessTokenService,
                 val sessionService: MongoService) {


  protected val orgService: OrgService = new OrgService {
    override def defaultCollection(o: Organization): Option[ObjectId] = {
      v1OrgService.getDefaultCollection(o.id) match {
        case Left(e) => None
        case Right(c) => Some(c.id)
      }
    }

    override def org(id: ObjectId): Option[Organization] = v1OrgService.findOneById(id)
  }

  protected val tokenService = new TokenService {
    override def orgForToken(token: String): Option[Organization] = {
      accessTokenService.findByToken(token).map(t => v1OrgService.findOneById(t.organization)).flatten
    }
  }

  protected val itemPermissionService : PermissionService[Organization, Item] = new PermissionService[Organization, Item] {
    override def create(client: Organization, newValue: Item): PermissionResult = {

      import scalaz.{Failure,Success}
      import scalaz.Scalaz._

      val result : Validation[String,PermissionResult] = for{
        id <- newValue.collectionId.toSuccess(s"No collection id specified in item: ${newValue.id}")
        contentCollection <- client.contentcolls.find(_.collectionId.toString == id).toSuccess(s"$id is not accessible to Organization: ${client.id}")
        collPermission <- Permission.fromLong(contentCollection.pval).toSuccess(s"Can't parse permission for collection: ${contentCollection.collectionId}")
        canWrite <- if(collPermission.has(Permission.Write)) Granted else Denied(s"${Permission.toHumanReadable(contentCollection.pval)} does not allow ${Permission.Write.name}")
      } yield canWrite


      result match {
        case Failure(msg) => Denied(msg)
        case Success(Granted) => Granted
      }
    }
  }

  protected lazy val apiActions: V2ApiActions[AnyContent] = new TokenAuthenticated {
    override def orgService: OrgService = Bootstrap.this.orgService
    override def tokenService: TokenService = Bootstrap.this.tokenService
  }


  private lazy val itemApi = new ItemApi {
    override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemService: ItemService = Bootstrap.this.itemService

    override def actions: V2ApiActions[AnyContent] = Bootstrap.this.apiActions

    override def orgService: OrgService = Bootstrap.this.orgService

    override def permissionService: PermissionService[Organization, Item] = Bootstrap.this.itemPermissionService
  }

  lazy val itemSessionApi = new ItemSessionApi {
    override def sessionService = Bootstrap.this.sessionService
  }

  lazy val controllers: Seq[Controller] = Seq(itemApi, itemSessionApi)
}
