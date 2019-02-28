package org.corespring.container.client.controllers.apps

import org.corespring.container.client.controllers.resources.routes._
import org.corespring.container.client.controllers.resources.{ routes => resourceRoutes }
import org.corespring.container.client.views.models.{ MainEndpoints, SessionEndpoints, SupportingMaterialsEndpoints }

trait Endpoints {
  def main(id: String): MainEndpoints
  def supportingMaterials(id: String): SupportingMaterialsEndpoints
}

object PlayerEndpoints {

  def session(sessionId: String) = SessionEndpoints(
    complete = Session.completeSession(sessionId),
    getScore = Session.getScore(sessionId),
    loadInstructorData = Session.loadInstructorData(sessionId),
    loadItemAndSession = Session.loadItemAndSession(sessionId),
    loadOutcome = Session.loadOutcome(sessionId),
    reopen = Session.reopenSession(sessionId),
    reset = Session.resetSession(sessionId),
    save = Session.saveSession(sessionId))
}

object ItemEditorEndpoints extends Endpoints {

  def supportingMaterials(itemId: String) = SupportingMaterialsEndpoints(
    addAsset = resourceRoutes.Item.addAssetToSupportingMaterial(itemId, ":name"),
    create = resourceRoutes.Item.createSupportingMaterial(itemId),
    createFromFile = resourceRoutes.Item.createSupportingMaterialFromFile(itemId),
    delete = resourceRoutes.Item.deleteSupportingMaterial(itemId, ":name"),
    deleteAsset = resourceRoutes.Item.deleteAssetFromSupportingMaterial(itemId, ":name", ":filename"),
    getAsset = resourceRoutes.Item.getAssetFromSupportingMaterial(itemId, ":name", ":filename"),
    updateContent = resourceRoutes.Item.updateSupportingMaterialContent(itemId, ":name", ":filename"))

  def main(itemId: String) = MainEndpoints(
    load = resourceRoutes.Item.load(itemId),
    save = None,
    saveSubset = resourceRoutes.Item.saveSubset(itemId, ":subset"),
    saveConfigXhtmlAndComponents = resourceRoutes.Item.saveConfigXhtmlAndComponents(itemId))
}

object DraftEditorEndpoints extends Endpoints {

  def supportingMaterials(id: String) = SupportingMaterialsEndpoints(
    addAsset = resourceRoutes.ItemDraft.addAssetToSupportingMaterial(id, ":name"),
    create = resourceRoutes.ItemDraft.createSupportingMaterial(id),
    createFromFile = resourceRoutes.ItemDraft.createSupportingMaterialFromFile(id),
    delete = resourceRoutes.ItemDraft.deleteSupportingMaterial(id, ":name"),
    deleteAsset = resourceRoutes.ItemDraft.deleteAssetFromSupportingMaterial(id, ":name", ":filename"),
    getAsset = resourceRoutes.ItemDraft.getAssetFromSupportingMaterial(id, ":name", ":filename"),
    updateContent = resourceRoutes.ItemDraft.updateSupportingMaterialContent(id, ":name", ":filename"))

  def main(id: String) = MainEndpoints(
    load = resourceRoutes.ItemDraft.load(id),
    save = Some(resourceRoutes.ItemDraft.save(id)),
    saveSubset = resourceRoutes.ItemDraft.saveSubset(id, ":subset"),
    saveConfigXhtmlAndComponents = resourceRoutes.ItemDraft.saveConfigXhtmlAndComponents(id))
}

