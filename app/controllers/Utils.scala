package controllers

import com.novus.salat.dao.SalatMongoCursor
import org.bson.types.ObjectId

object Utils {
  def toObjectId(id:String):Option[ObjectId] = {
    try{
      Some(new ObjectId(id))
    } catch {
      case e:IllegalArgumentException => None
    }
  }

  /** This method will be removed soon once 'cursor.toSeq' has been deemed ok */
  def toSeq[T <: AnyRef](c:SalatMongoCursor[T]):Seq[T] = c.toSeq

  private def removeWhitespace(s: String):String = {
    s.replaceAll("\\s","").replaceAll("\"","'")
  }

  def getLevenshteinDistance (ws: String, wt:String):Double = {
    if (ws == null || wt == null) throw new IllegalArgumentException("Strings must not be null")

    val s = removeWhitespace(ws)
    val t = removeWhitespace(wt)

    val n = s.length()
    val m = t.length()

    if (n == 0) return m

    else if (m == 0) return n

    var p = new Array[Int](n+1)
    var d = new Array[Int](n+1)

    for (i <- 0 to n) {
      p.update(i,i)
    }

    for (j <- 1 to m) {
      val t_j = t.charAt(j-1)
      d.update(0,j)

      for (i <- 1 to n) {
        val cost:Int = if (s.charAt(i-1)==t_j) 0 else 1
        d.update(i,math.min(math.min(d(i-1)+1, p(i)+1), p(i-1)+cost))
      }
      val _d = p
      p = d
      d = _d
    }

    //Determine percentage difference
    val levNum = p(n).asInstanceOf[Double]
    val percent = (levNum/math.max(s.length(),t.length()))*100
    percent
  }
}
case class JsonValidationException(field:String) extends RuntimeException("invalid value for: "+field)
