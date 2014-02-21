package api.v1

import org.corespring.platform.core.models.item.Content
import controllers.auth.BaseApi
import play.api.mvc._

abstract class ContentApi[C <: Content[_]] extends BaseApi {

  def list(query: Option[String], fields: Option[String], count: String, skip: Int, limit: Int,
           sort: Option[String]): Action[AnyContent]

  def listAndCount(query: Option[String], fields: Option[String], skip: Int, limit: Int,
                   sort: Option[String]): Action[AnyContent]

}
