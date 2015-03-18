package org.corespring.v2.player.controllers

import org.bson.types.ObjectId
import org.corespring.container.client.controllers.PlayerLauncher
import org.corespring.container.client.views.txt.js.ServerLibraryWrapper
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ Action }

trait EditorJs extends PlayerLauncher {

  lazy val apiEditor = ServerLibraryWrapper("api-editor",
    """|function ApiEditor(element, options, errorCallback){
                                              |
                                              |  var builder = new (require('url-builder'))();
                                                 var queryParams = require('query-params');
                                              |
                                              |
                                              |  function makeUrl(url, queryParams) {
                                              |    return builder.build(url, queryParams);
                                              |  }
                                              |
                                              |  var defaultOptions = require("default-options");
                                              |  var paths = defaultOptions.paths;
                                              |
                                              |  if(options.itemId){
                                              |     var url = paths.drafts.create.url.replace(':itemId', options.itemId);
                                              |
                                              |     $.ajax({
                                              |      type: paths.drafts.create.method,
                                              |      url: makeUrl(defaultOptions.corespringUrl + url, queryParams),
                                              |      success: function(result){
                                              |        var draftId = result.id;
                                              |        new require('editor')(element, { itemId: draftId }, errorCallback);
                                              |      },
                                              |      error: function(err){
                                              |        errorCallback({code: 112, msg: 'Error creating draft'});
                                              |      }
                                              |     });
                                              |   } else if(options.draftId){
                                              |       new require('editor')(element, { itemId: draftId }, errorCallback);
                                              |   }
                                              |
                                              |  this.commitDraft = function(callback){
                                              |    var url = paths.drafts.commit.url.replace(':draftId', options.draftId);
                                              |    var method = paths.drafts.commit.method;
                                              |
                                              |    function onSuccess(result){
                                              |      callback(null) ;
                                              |    }
                                              |
                                              |    function onError(err){
                                              |      callback({code: 111, msg: 'Failed to commit draft'})
                                              |    }
                                              |
                                              |    $.ajax({
                                              |      type: method,
                                              |      url: makeUrl(defaultOptions.corespringUrl + url, queryParams),
                                              |      data: options,
                                              |      success: onSuccess,
                                              |      error: onError,
                                              |      dataType: "json"
                                              |    });
                                              |  }
                                              |}
                                              |
                                              |module.exports = ApiEditor;""")
  override def editorJs = Action.async { implicit request =>

    import org.corespring.container.client.controllers.apps.routes.Editor
    val loadEditorCall = Editor.load(":itemId")

    hooks.editorJs.map { implicit js =>

      val rootUrl = playerConfig.rootUrl.getOrElse(BaseUrl(request))
      val create = org.corespring.container.client.controllers.resources.routes.Item.create()

      val commit = {
        val oid = ObjectId.get
        val call = org.corespring.v2.api.drafts.item.routes.ItemDrafts.commit(oid)
        call.copy(url = call.url.replace(oid.toString, ":draftId"))
      }

      val defaultOptions: JsValue = Json.obj(
        "corespringUrl" -> rootUrl,
        "paths" -> Json.obj(
          "drafts" -> Json.obj(
            "create" -> org.corespring.v2.api.drafts.item.routes.ItemDrafts.create(":itemId"),
            "commit" -> commit),
          "editor" -> loadEditorCall,
          "create" -> create))
      val finalJs = buildJs(editorNameAndSrc, defaultOptions, "")

      val csApiJs =
        s"""
          |$finalJs
          |//Cs-api override
          |$apiEditor
          |org.corespring.players.ItemEditor = corespring.require('api-editor');
        """.stripMargin
      addSession(Ok(csApiJs), js)
    }
  }

}
