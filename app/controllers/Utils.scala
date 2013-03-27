package controllers

import com.novus.salat.dao.SalatMongoCursor
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


  private def removeWhitespace(s: String):String = {
    s.replaceAll("\\s","").replaceAll("\"","'")
  }
  def getLevenshteinDistance (ws: String, wt:String):Double = {
    if (ws == null || wt == null) throw new IllegalArgumentException("Strings must not be null");

    val s = removeWhitespace(ws)
    val t = removeWhitespace(wt)

    val n = s.length();
    val m = t.length();

    if (n == 0) return m;

    else if (m == 0) return n;

    var p = new Array[Int](n+1);
    var d = new Array[Int](n+1);

    for (i <- 0 to n) {
      p.update(i,i)
    }

    for (j <- 1 to m) {
      val t_j = t.charAt(j-1);
      d.update(0,j);

      for (i <- 1 to n) {
        val cost:Int = if (s.charAt(i-1)==t_j) 0 else 1;
        d.update(i,math.min(math.min(d(i-1)+1, p(i)+1), p(i-1)+cost))
      }
      val _d = p;
      p = d;
      d = _d;
    }

    //Determine percentage difference
    val levNum = p(n).asInstanceOf[Double];
    val percent = (levNum/math.max(s.length(),t.length()))*100;

    return percent;
  }
}
case class JsonValidationException(field:String) extends RuntimeException("invalid value for: "+field)
