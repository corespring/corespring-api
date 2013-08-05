package controllers

import collection.generic.CanBuildFrom
import collection.mutable.ArrayBuffer
import org.bson.types.ObjectId
import scala.language.higherKinds
import scala.language.postfixOps
import scala.util.Random
import scala.xml.Node

object Utils {

  def isTrue(v:Any):Boolean = {
    v match {
      case s:String => s.toLowerCase == "true"
      case b:Boolean => b
      case n:Node => isTrue(n.text)
      case Some(some) => isTrue(some)
      case _ => false
    }
  }

  def shuffle[T, CC[X] <: TraversableOnce[X]](xs: CC[T], isFixed: T => Boolean)(implicit bf: CanBuildFrom[CC[T], T, CC[T]]): CC[T] = {
    val buf = new ArrayBuffer[T] ++= xs

    def swap(i1: Int, i2: Int) {
      if (!isFixed(buf(i1)) && !isFixed(buf(i2))) {
        val tmp = buf(i1)
        buf(i1) = buf(i2)
        buf(i2) = tmp
      }
    }

    def getRandomIndex(n: Int): Int = {
      if (buf.take(n).forall(isFixed(_)))
        -1
      else {
        val index = Random.nextInt(n)
        if (!isFixed(buf(index))) index else getRandomIndex(n)
      }
    }

    for (n <- buf.length to 2 by -1) {
      if (!isFixed(buf(n - 1))) {
        val k = getRandomIndex(n)
        if (k >= 0) swap(n - 1, k)
      }
    }

    bf(xs) ++= buf result
  }

  def toObjectId(id: String): Option[ObjectId] = {
    if(ObjectId.isValid(id)){
      Some(new ObjectId(id))
    } else {
      None
    }
  }

  private def removeWhitespace(s: String): String = {
    s.replaceAll("\\s", "").replaceAll("\"", "'")
  }

  def getLevenshteinDistance(ws: String, wt: String): Double = {
    if (ws == null || wt == null) throw new IllegalArgumentException("Strings must not be null")

    val s = removeWhitespace(ws)
    val t = removeWhitespace(wt)

    val n = s.length()
    val m = t.length()

    if (n == 0) return m

    else if (m == 0) return n

    var p = new Array[Int](n + 1)
    var d = new Array[Int](n + 1)

    for (i <- 0 to n) {
      p.update(i, i)
    }

    for (j <- 1 to m) {
      val t_j = t.charAt(j - 1)
      d.update(0, j)

      for (i <- 1 to n) {
        val cost: Int = if (s.charAt(i - 1) == t_j) 0 else 1
        d.update(i, math.min(math.min(d(i - 1) + 1, p(i) + 1), p(i - 1) + cost))
      }
      val _d = p
      p = d
      d = _d
    }

    //Determine percentage difference
    val levNum = p(n).asInstanceOf[Double]
    val percent = (levNum / math.max(s.length(), t.length())) * 100
    percent
  }
}
//TODO: move this out of common-utils
case class JsonValidationException(field: String) extends RuntimeException("invalid value for: " + field)
