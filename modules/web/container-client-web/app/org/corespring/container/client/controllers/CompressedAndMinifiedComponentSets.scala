package org.corespring.container.client.controllers

import java.io.File

import org.apache.commons.io.FileUtils
import org.corespring.container.client.component.{ ComponentSetExecutionContext, ComponentsConfig }
import org.corespring.container.client.io.ResourcePath
import org.corespring.container.client.processing.{ CssMinifier, Gzipper, JsMinifier }
import org.corespring.container.components.model.Component
import org.corespring.container.components.services.{ ComponentService, DependencyResolver }
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc._
import javax.xml.bind.DatatypeConverter

import scala.concurrent.Future

class CompressedAndMinifiedComponentSets(componentSetExecutionContext: ComponentSetExecutionContext,
  componentService: ComponentService,
  resourceLoader: ResourcePath,
  config: ComponentsConfig,
  val dependencyResolver: DependencyResolver) extends DefaultComponentSets

  with JsMinifier with CssMinifier with Gzipper {

  private lazy val logger = Logger(classOf[CompressedAndMinifiedComponentSets])

  private val minifyEnabled = config.minify

  private val gzipEnabled = config.gzip

  val componentsConfig = config

  private def acceptsGzip(implicit rh: RequestHeader): Boolean = {
    rh.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_.trim == "gzip")).getOrElse(false)
  }

  def process(s: String, contentType: String)(implicit rh: RequestHeader): SimpleResult = {

    logger.warn(s"function=process - this is an expensive operation! use sparingly.")
    logger.trace(s"process minify? $minifyEnabled, gzip? $gzipEnabled")

    val out: Either[String, String] = contentType match {
      case ss: String if ss == ContentTypes.JAVASCRIPT => if (minifyEnabled) minifyJs(s) else Right(s)
      case ss: String if ss == ContentTypes.CSS => if (minifyEnabled) minifyCss(s) else Right(s)
      case _ => Left(s"unknown content type: $contentType")
    }
    out match {
      case Right(s) => {
        if (gzipEnabled && acceptsGzip) {
          val zipped = gzip(s)
          Ok(zipped).as(contentType).withHeaders(CONTENT_ENCODING -> "gzip", CONTENT_LENGTH -> zipped.length.toString)
        } else {
          Ok(s).as(contentType).withHeaders(CONTENT_LENGTH -> s.getBytes("UTF-8").length.toString)
        }
      }
      case Left(err) => BadRequest(err)
    }
  }

  private def tokenToJson(token: String): JsObject = {
    val decodedColorString = DatatypeConverter.parseBase64Binary(token).map(_.toChar).mkString
    try {
      Json.parse(decodedColorString).as[JsObject]
    } catch {
      case _ => Json.obj()
    }
  }

  override def singleResource[A >: EssentialAction](context: String, componentType: String, suffix: String): A = Action.async {
    implicit request =>
      Future {
        val resourceToken = request.queryString.get("resourceToken")
        val paramsJson = resourceToken match {
          case Some(token) if (token.length > 0) => tokenToJson(token(0))
          case _ => Json.obj()
        }
        val (body, ct) = generate(context, allComponents.find(_.matchesType(componentType)).toSeq, suffix, paramsJson)
        process(body, ct)
      }(componentSetExecutionContext.heavyLoad)
  }

  override def resource[A >: EssentialAction](context: String, directive: String, suffix: String) = Action.async {
    implicit request =>
      Future {
        val resourceToken = request.queryString.get("resourceToken")
        val paramsJson = resourceToken match {
          case Some(token) if (token.length > 0) => tokenToJson(token(0))
          case _ => Json.obj()
        }
        val (body, ct) = generateBodyAndContentType(context, directive, suffix, paramsJson)
        process(body, ct)
      }(componentSetExecutionContext.heavyLoad)
  }

  override def loadLibrarySource(path: String): Option[String] = {
    val componentsPath = config.componentsPath
    val fullPath = s"$componentsPath/$path"
    val file = new File(fullPath)

    if (file.exists()) {
      logger.trace(s"load file: $path")
      Some(FileUtils.readFileToString(file, "UTF-8"))
    } else {
      Some(s"console.warn('failed to log $fullPath');")
    }
  }

  override def resource(path: String): Option[String] = {
    val fullPath = s"${config.bowerComponentsPath}/$path"
    val out = resourceLoader.loadPath(fullPath)

    if (out.isEmpty) {
      logger.warn(s"function=resource, fullPath=$fullPath - failed to load")
    }
    out
  }

  override def allComponents: Seq[Component] = componentService.components
}

