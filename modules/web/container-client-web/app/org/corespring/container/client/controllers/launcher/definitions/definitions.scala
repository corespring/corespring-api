package org.corespring.container.client.controllers.launcher

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.hooks.PlayerJs
import play.api.http.ContentTypes
import play.api.libs.json.{ JsObject }
import play.api.libs.json.Json._
import play.api.mvc.{ Call, RequestHeader, Session, SimpleResult }

private[launcher] trait LaunchCompanionUtils {
  def params(rh: RequestHeader) = rh.queryString.mapValues(_.mkString(""))
}

private object Implicits {
  implicit def callToJsv(c: Call): JsValueWrapper = toJsFieldJsValueWrapper(obj("method" -> c.method, "url" -> c.url))
}

import org.corespring.container.client.controllers.launcher.Implicits._

trait CorespringJsClient {

  def initTimeout: Int
  def builder: JsBuilder
  def fileNames: Seq[String]
  def bootstrap: String
  def options: JsObject
  def queryParams: Map[String, String]

  def src(corespringUrl: String) = {
    val finalOpts = options.deepMerge(obj("initTimeout" -> initTimeout))
    builder.buildJs(corespringUrl, fileNames, finalOpts, bootstrap, queryParams)
  }

  def result(corespringUrl: String): SimpleResult = {
    import play.api.mvc.Results.Ok
    Ok(src(corespringUrl)).as(ContentTypes.JAVASCRIPT)
  }
}

private[launcher] object Catalog extends LaunchCompanionUtils {
  def apply(config: V2PlayerConfig, rh: RequestHeader, builder: JsBuilder): Catalog = {
    Catalog(config.launchTimeout, builder, params(rh))
  }
}

private[launcher] case class Catalog(initTimeout: Int, val builder: JsBuilder, queryParams: Map[String, String]) extends CorespringJsClient {
  override def fileNames: Seq[String] = Seq("catalog.js")

  override def bootstrap: String =
    """
      |org.corespring.players.ItemCatalog = corespring.require('catalog');
    """.stripMargin

  import org.corespring.container.client.controllers.apps.routes.{ Catalog => Routes }

  override def options: JsObject = obj(
    "paths" -> obj(
      "catalog" -> Routes.load(":itemId")))
}

private[launcher] object Player extends LaunchCompanionUtils {
  def apply(config: V2PlayerConfig, rh: RequestHeader, playerJs: PlayerJs, builder: JsBuilder): Player = {
    Player(config.launchTimeout, builder, params(rh), playerJs)
  }
}

private[launcher] case class Player(initTimeout: Int, builder: JsBuilder, queryParams: Map[String, String], playerJs: PlayerJs) extends CorespringJsClient {
  override lazy val fileNames: Seq[String] = Seq("player.js")

  override lazy val bootstrap: String =
    s"""
      |org.corespring.players.ItemPlayer = corespring.require('player').define(${playerJs.isSecure});
    """.stripMargin

  override lazy val options: JsObject = {

    val errorsAndWarnings = obj("errors" -> playerJs.errors, "warnings" -> playerJs.warnings)

    import org.corespring.container.client.controllers.apps.routes.{ Player => Routes }

    val loadSession = Routes.loadBySession(":sessionId")

    val paths = obj("paths" -> obj(
      "createSession" -> Routes.createSessionForItem(":id"),
      "gather" -> loadSession,
      "view" -> loadSession,
      "evaluate" -> loadSession))

    errorsAndWarnings.deepMerge(paths)
  }

  protected def sumSession(s: Session, keyValues: (String, String)*): Session = keyValues.foldRight(s) { case ((key, value), acc) => acc + (key -> value) }
  val SecureMode = "corespring.player.secure"

  override def result(corespringUrl: String): SimpleResult = {
    val finalSession = sumSession(playerJs.session, (SecureMode, playerJs.isSecure.toString))
    super.result(corespringUrl).withSession(finalSession)
  }
}

private[launcher] object ItemEditors extends LaunchCompanionUtils {

  def apply(config: V2PlayerConfig, rh: RequestHeader, builder: JsBuilder): ItemEditors = {
    ItemEditors(config.launchTimeout, builder, params(rh))
  }
}

private[launcher] case class ItemEditors(initTimeout: Int, builder: JsBuilder, queryParams: Map[String, String]) extends CorespringJsClient {
  import org.corespring.container.client.controllers.apps.routes.{ DraftDevEditor => DraftDevEditorRoutes, DraftEditor => DraftEditorRoutes, ItemDevEditor => ItemDevEditorRoutes, ItemEditor => ItemEditorRoutes }
  import org.corespring.container.client.controllers.resources.routes.{ Item => ItemRoutes, ItemDraft => ItemDraftRoutes }

  val paths: JsObject = obj(
    "itemEditor" -> obj(
      "editor" -> ItemEditorRoutes.load(":itemId"),
      "devEditor" -> ItemDevEditorRoutes.load(":itemId"),
      "createItem" -> ItemRoutes.create()),
    "draftEditor" -> obj(
      "editor" -> DraftEditorRoutes.load(":draftId"),
      "devEditor" -> DraftDevEditorRoutes.load(":draftId"),
      "createItemAndDraft" -> ItemDraftRoutes.createItemAndDraft(),
      "commitDraft" -> ItemDraftRoutes.commit(":draftId"),
      "save" -> ItemDraftRoutes.save(":draftId")))

  override lazy val options: JsObject = obj("paths" -> paths)

  override val fileNames: Seq[String] = Seq("item-editor.js", "draft.js", "draft-editor.js")

  override val bootstrap: String =
    """
      |org.corespring.players.ItemEditor = corespring.require('item-editor');
      |org.corespring.players.DraftEditor = corespring.require('draft-editor');
      |org.corespring.editors.ItemEditor = corespring.require('item-editor');
      |org.corespring.editors.DraftEditor = corespring.require('draft-editor');
      |
    """.stripMargin
}

private[launcher] object ComponentEditor extends LaunchCompanionUtils {
  def apply(config: V2PlayerConfig, rh: RequestHeader, builder: JsBuilder): ComponentEditor = {
    ComponentEditor(config.launchTimeout, builder, params(rh))
  }
}

private[launcher] case class ComponentEditor(initTimeout: Int, builder: JsBuilder, queryParams: Map[String, String]) extends CorespringJsClient {

  override val fileNames = Seq("draft.js", "component-editor.js")

  override val bootstrap =
    """
      |var modules = corespring.require('component-editor');
      |org.corespring.players.ComponentEditor = modules.Standalone;
      |org.corespring.players.ItemComponentEditor = modules.Item;
      |org.corespring.players.DraftComponentEditor = modules.Draft;
      |org.corespring.players.QuestionComponentEditor = modules.QuestionComponentEditor;
      |org.corespring.editors.ComponentEditor = modules.Standalone;
      |org.corespring.editors.ItemComponentEditor = modules.Item;
      |org.corespring.editors.DraftComponentEditor = modules.Draft;
      |org.corespring.editors.QuestionComponentEditor = modules.QuestionComponentEditor;
    """.stripMargin

  private object r {

    import org.corespring.container.client.controllers.{ apps, resources }

    val componentEditor = apps.routes.ComponentEditor
    val itemEditor = apps.routes.ItemEditor
    val draftEditor = apps.routes.DraftEditor
    val item = resources.routes.Item
    val draft = resources.routes.ItemDraft
  }

  override lazy val options: JsObject = obj(
    "paths" -> obj(
      "standaloneEditor" -> r.componentEditor.load(":componentType"),
      "itemEditor" -> obj(
        "singleComponent" -> obj(
          "createWithSingleComponent" -> r.item.createWithSingleComponent(":componentType"),
          "loadData" -> r.item.load(":itemId"),
          "loadEditor" -> r.itemEditor.componentEditor(":itemId"),
          "upload" -> r.itemEditor.uploadFile(":itemId", ":filename"),
          "saveComponents" -> r.item.saveSubset(":itemId", "components"),
          "saveConfigXhtmlAndComponents" -> r.item.saveConfigXhtmlAndComponents(":itemId"))),
      "draftEditor" -> obj(
        "singleComponent" -> obj(
          "createWithSingleComponent" -> r.draft.createWithSingleComponent(":componentType"),
          "commit" -> r.draft.commit(":draftId"),
          "loadData" -> r.draft.load(":draftId"),
          "loadEditor" -> r.draftEditor.componentEditor(":draftId"),
          "upload" -> r.draftEditor.uploadFile(":draftId", ":filename"),
          "saveComponents" -> r.draft.saveSubset(":draftId", "components"),
          "saveConfigXhtmlAndComponents" -> r.draft.saveConfigXhtmlAndComponents(":draftId")))))

}