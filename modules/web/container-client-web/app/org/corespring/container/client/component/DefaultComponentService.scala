package org.corespring.container.client.component

import org.corespring.container.components.model.Component
import org.corespring.container.components.services.ComponentService
import play.api.Mode
import play.api.Mode._

class DefaultComponentService(mode: Mode, loadComps: => Seq[Component]) extends ComponentService {

  private var loadedComponents: Seq[Component] = Seq.empty

  override def components: Seq[Component] = (mode, loadedComponents) match {
    case (Mode.Prod, Nil) => {
      loadedComponents = loadComps
      loadedComponents
    }
    case (Mode.Prod, x :: _) => loadedComponents
    case _ => loadComps
  }
}
