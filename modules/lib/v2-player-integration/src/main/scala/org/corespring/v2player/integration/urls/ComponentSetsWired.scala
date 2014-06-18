package org.corespring.v2player.integration.urls

import org.corespring.container.client.component.{ CatalogGenerator, EditorGenerator, PlayerGenerator, SourceGenerator }
import org.corespring.container.client.controllers.ComponentSets
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import play.api.cache.Cached
import play.api.{ Mode, Play }

trait ComponentSetsWired extends ComponentSets {

  override def dependencyResolver: DependencyResolver = new DependencyResolver {
    override def components: Seq[Component] = ComponentSetsWired.this.allComponents
  }

  override def resource[A >: play.api.mvc.EssentialAction](context: scala.Predef.String, directive: scala.Predef.String, suffix: scala.Predef.String): A = {
    if (Play.current.mode == Mode.Dev) {
      super.resource(context, directive, suffix)
    } else {
      implicit val current = play.api.Play.current
      Cached(s"$context-$directive-$suffix") {
        super.resource(context, directive, suffix)
      }
    }
  }

  override def editorGenerator: SourceGenerator = new EditorGenerator

  override def playerGenerator: SourceGenerator = new PlayerGenerator

  override def catalogGenerator: SourceGenerator = new CatalogGenerator
}
