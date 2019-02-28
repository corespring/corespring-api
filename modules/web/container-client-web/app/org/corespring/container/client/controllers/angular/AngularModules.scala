package org.corespring.container.client.controllers.angular

import org.corespring.container.client.controllers.helpers.NameHelper
import org.corespring.container.components.model.packaging.ClientSideDependency
import org.corespring.container.components.model._

class AngularModules(defaultModules: String*) extends NameHelper {

  private def hasClientSideLogic(c: Component): Boolean = c match {
    case i: Interaction => true
    case w: Widget => true
    case l: Library => l.client.size > 0
    case y: LayoutComponent => true
    case _ => false
  }

  def createAngularModules(components: Seq[Component], clientSideDependencies: Seq[ClientSideDependency]): Seq[String] = {
    val clientSideComponents = components.filter(hasClientSideLogic)
    val compModules: Seq[String] = clientSideComponents.map(c => idToModuleName(c.id))
    val dependencyModules: Seq[String] = clientSideDependencies.flatMap(_.angularModule)
    (defaultModules ++ compModules ++ dependencyModules).sorted.distinct
  }

  protected def idToModuleName(id: Id): String = moduleName(id.org, id.name)
}

