package controllers

import com.novus.salat.dao.SalatMongoCursor

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
    val seq = c.foldRight[Seq[T]](Seq())((o,acc) => acc :+ o)
    c.close()
    seq
  }
}
