package org.corespring.poc.integration

import play.api.mvc.Controller
import play.api.{Logger, GlobalSettings}
import scala.collection._
import org.slf4j.LoggerFactory

trait ControllerInstanceResolver extends GlobalSettings {

  private val mappedTypes : mutable.Map[String, Controller] = new mutable.HashMap()

  def controllers : Seq[Controller]

  override def getControllerInstance[A](controllerClass: Class[A]): A = {

    def isSuper(test: Class[_], target: Class[A]): Boolean = {

      if (test == null) {
        return false
      }
      if (test.getSuperclass == target) {
        true
      } else {
        if ( test.getSuperclass == null || test.getSuperclass.getName == "java.lang.Object") {
          false
        } else {
          isSuper(test.getSuperclass, target)
        }
      }
    }

    def isCorrectType(p: Class[_]): Boolean =  {
      p == controllerClass
    }

    //TODO: tidy up
    def matches(c: Class[_]) : Boolean = {

      if(c == null){
        return false
      }

      if(c.getName == "java.lang.Object"){
        return false
      }

      if( c == controllerClass ){
        true
      } else {
        val interfaces = c.getInterfaces.toList
        val existsAsInterface = interfaces.exists(isCorrectType)
        if(existsAsInterface){
          return true
        } else {
          if( matches(c.getSuperclass) ){
            return true
          }
          else if( interfaces.exists(matches)){
            return true
          }
        }
      }
      false
    }

    def searchTypeHierarchy = {
      controllers.find {
        (c: Controller) => {
          val cType = c.asInstanceOf[Object].getClass
          val m = matches(cType)
          m
        }
      } match {
        case Some(c) => {
          Logger.debug("Found an implementation for " + controllerClass + ": " + c)
          mappedTypes.put(controllerClass.getCanonicalName, c)
          c.asInstanceOf[A]
        }
        case _ => throw new RuntimeException("Can't find controller for: " + controllerClass)
      }

    }

    mappedTypes.get(controllerClass.getCanonicalName).map{ c =>
      c.asInstanceOf[A]
    }.getOrElse{
      searchTypeHierarchy
    }
  }
}

