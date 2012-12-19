package controllers

import com.novus.salat.dao.SalatMongoCursor
import collection.mutable
import com.mongodb.casbah.Imports._
import scala.Left
import com.novus.salat.dao.SalatMongoCursor
import scala.Right
import org.bson.types.ObjectId
import com.mongodb.util.JSON
import models.QueryField
import play.api.libs.json.{JsString, JsObject}
import xml.{Elem, Node}

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/17/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */

object Utils {
  /**
   * return a sequence of object T's. closes the cursor after the sequence has been computed
   * @param c
   * @tparam T
   * @return
   */
  def toSeq[T <: AnyRef](c:SalatMongoCursor[T]):Seq[T] = {
    val seq = c.foldRight[Seq[T]](Seq())((o,acc) => o +: acc)
    c.close()
    seq
  }

  /**
   * traverse the given xml and apply the accumulator function to each element.
   * if the accumulator function returns Some(Seq[T]), then the resulting sequence will appended to accumulator and traverse the next node on that element's level.
   * If it returns None, then it will continue to traverse within that element
   * @param node - the given xml
   * @param accFn - accumulator function
   * @tparam T - the object type of the returned sequence of objects
   * @return -  accumulator
   */
  def traverseElements[T](node:Node)(accFn:(Elem)=>Option[Seq[T]]):Seq[T] = {
    traverseElementsRec(node,accFn,Seq())
  }
  private def traverseElementsRec[T](node:Node, accFn:(Elem)=>Option[Seq[T]], acc:Seq[T]):Seq[T] = {
    node match {
      case e:Elem => accFn(e) match {
        case Some(accadd) => acc ++ accadd
        case None => e.child.map(child => traverseElementsRec(child,accFn,acc)).flatten
      }
      case other => other.child.map(child => traverseElementsRec(child,accFn,acc)).flatten
    }
  }
}
case class JsonValidationException(field:String) extends RuntimeException("invalid value for: "+field)
