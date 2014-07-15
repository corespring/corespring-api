package org.corespring.v2.player.urls

import org.corespring.container.client.component.{ CatalogGenerator, EditorGenerator, PlayerGenerator, SourceGenerator }
import org.corespring.container.client.controllers.ComponentSets
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import play.api.cache.Cache
import play.api.mvc._
import play.api.{ Mode, Play }

trait ComponentSetsWired extends ComponentSets {

  override def dependencyResolver: DependencyResolver = new DependencyResolver {
    override def components: Seq[Component] = ComponentSetsWired.this.allComponents
  }

  override def resource[A >: play.api.mvc.EssentialAction](context: scala.Predef.String, directive: scala.Predef.String, suffix: scala.Predef.String): A = {

    val bodyAndContentType: (String, String) = {

      implicit val current = play.api.Play.current

      if (Play.current.mode == Mode.Dev) {
        generateBodyAndContentType(context, directive, suffix)
      } else {
        val cacheKey = s"$context-$directive-$suffix"
        Cache.get(cacheKey) match {
          case Some(value) => value.asInstanceOf[(String, String)]
          case None =>
            val out = generateBodyAndContentType(context, directive, suffix)
            Cache.set(cacheKey, out)
            out
        }
      }
    }

    val (body, ct) = bodyAndContentType
    Action(Ok(body).as(ct))
  }

  override def editorGenerator: SourceGenerator = new EditorGenerator

  override def playerGenerator: SourceGenerator = new PlayerGenerator

  override def catalogGenerator: SourceGenerator = new CatalogGenerator
}
