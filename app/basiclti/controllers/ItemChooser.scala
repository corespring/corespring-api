package basiclti.controllers

import play.api.mvc.{Action, Controller}

object ItemChooser extends Controller {

  case class Params(selectionDirective:String, launchPresentationReturnUrl:String)

  object Params {

    def apply(form:Map[String,Seq[String]]) : Params = {
      new Params(
      selectionDirective = form.get("selection_directive").getOrElse(Seq("")).head,
      launchPresentationReturnUrl = form.get("launch_presentation_return_url").getOrElse(Seq("")).head
      )
    }
  }
  def choose = Action{ request =>

    val params = Params(request.body.asFormUrlEncoded.get)

    val call = basiclti.controllers.routes.AssignmentLauncher.launchById(None)
    Ok(basiclti.views.html.itemChooser(params.selectionDirective, params.launchPresentationReturnUrl, call.url))
  }
}
