package org.corespring.container.client.component

import org.corespring.container.client.controllers.angular.AngularModules
import org.corespring.container.client.controllers.helpers.LoadClientSideDependencies
import org.corespring.container.components.model.{ ComponentInfo, Id }
import org.corespring.container.components.services.{ ComponentService, DependencyResolver }

trait ComponentBundler {
  def singleBundle(componentType: String, context: String, expandPaths: Boolean, customColorsEncoded: Option[String] = None): Option[SingleComponentScriptBundle]

  def bundleAll(context: String, scope: Option[String], expandPaths: Boolean, customColorsEncoded: Option[String] = None): Option[ComponentsScriptBundle]
  def bundle(ids: Seq[Id], context: String, scope: Option[String], expandPaths: Boolean, customColorsEncoded: Option[String] = None): Option[ComponentsScriptBundle]
}

class DefaultComponentBundler(
  dependencyResolver: DependencyResolver,
  clientSideDependencies: LoadClientSideDependencies,
  urls: ComponentUrls,
  componentService: ComponentService)
  extends ComponentBundler {

  override def singleBundle(componentType: String, context: String, expandPaths: Boolean = false, customColorsEncoded: Option[String] = None): Option[SingleComponentScriptBundle] = {

    componentService.components.find(_.componentType == componentType).map { c =>
      val resolved = dependencyResolver.resolveComponents(Seq(c.id), Some(context))

      val cd = clientSideDependencies.getClientSideDependencies(resolved)
      val ngModules = new AngularModules().createAngularModules(resolved, cd)
      SingleComponentScriptBundle(
        component = c.asInstanceOf[ComponentInfo],
        urls.jsUrl(context, resolved, expandPaths),
        urls.lessUrl(context, resolved, expandPaths, customColorsEncoded),
        ngModules)
    }
  }

  override def bundle(ids: Seq[Id], context: String, scope: Option[String], expandPaths: Boolean, customColorsEncoded: Option[String] = None) = {
    val resolved = dependencyResolver.resolveComponents(ids, scope)
    val cd = clientSideDependencies.getClientSideDependencies(resolved)
    val ngModules = new AngularModules().createAngularModules(resolved, cd)
    Some(ComponentsScriptBundle(
      components = resolved,
      urls.jsUrl(context, resolved, expandPaths),
      urls.lessUrl(context, resolved, expandPaths, customColorsEncoded),
      ngModules))

  }

  override def bundleAll(context: String, scope: Option[String], expandPaths: Boolean, customColorsEncoded: Option[String] = None): Option[ComponentsScriptBundle] = {
    bundle(componentService.components.map(_.id), context, scope, expandPaths, customColorsEncoded)
  }
}
