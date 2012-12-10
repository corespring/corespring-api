package basiclti.controllers

import play.api.mvc.{Action, Controller}
import common.controllers.utils.BaseUrl
import basiclti.models.LtiLaunchConfiguration
import com.mongodb.casbah.commons.MongoDBObject
import models.{ItemSessionSettings, ItemSession, Item}
import org.bson.types.ObjectId

object ItemChooser extends Controller {

  case class Params(
                     selectionDirective:String,
                     launchPresentationReturnUrl:String,
                     resourceLinkId:String)

  object Params {

    def apply(form:Map[String,Seq[String]]) : Params = {
      new Params(
      selectionDirective = form.get("selection_directive").getOrElse(Seq("")).head,
      launchPresentationReturnUrl = form.get("launch_presentation_return_url").getOrElse(Seq("")).head,
      resourceLinkId = form.get("resource_link_id").getOrElse(Seq("")).head
      )
    }
  }

  private def getOrCreateConfig(params:Params) : LtiLaunchConfiguration = {
    LtiLaunchConfiguration.findByResourceLinkId(params.resourceLinkId) match {
      case Some(c) => c
      case _ => {
        val newConfig = new LtiLaunchConfiguration(
          resourceLinkId = params.resourceLinkId,
          itemId = None,
          sessionSettings = Some(new ItemSessionSettings()))
        LtiLaunchConfiguration.save(newConfig)
        newConfig
      }
    }
  }

  /**
   * Just for development - to be removed.
   * @return
   */
  def mockLauncher = Action{ request =>
    val url = basiclti.controllers.routes.ItemChooser.choose().url
    Ok(basiclti.views.html.dev.launchItemChooser(url))
  }


  def choose = Action{ request =>
    val params = Params(request.body.asFormUrlEncoded.get)
    val config = getOrCreateConfig(params)
    val call = basiclti.controllers.routes.AssignmentLauncher.launchById(None)
    Ok(basiclti.views.html.itemChooser(config.id, params.selectionDirective, params.launchPresentationReturnUrl, call.url))
  }

  def xml(title:String, description:String, url:String, width:Int, height:Int) = {
    <cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0" xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0" xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0" xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0.xsd http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:title>{title}</blti:title>
      <blti:description>{description}</blti:description>
      <blti:extensions platform="canvas.instructure.com">
        <lticm:property name="tool_id">corespring_resource_selection</lticm:property>
        <lticm:property name="privacy_level">anonymous</lticm:property>
        <lticm:options name="resource_selection">
          <lticm:property name="url">{url}</lticm:property>
          <lticm:property name="text">???</lticm:property>
          <lticm:property name="selection_width">{width}</lticm:property>
          <lticm:property name="selection_height">{height}</lticm:property>
        </lticm:options>
      </blti:extensions>
      <cartridge_bundle identifierref="BLTI001_Bundle"/>
      <cartridge_icon identifierref="BLTI001_Icon"/>
    </cartridge_basiclti_link>
  }

  def xmlConfiguration = Action{ request =>
    val url = basiclti.controllers.routes.ItemChooser.choose().url
    val root = BaseUrl(request)
    Ok(xml("item-chooser", "choose an item", root + url, 600, 500)).withHeaders((CONTENT_TYPE, "application/xml"))
  }
}
