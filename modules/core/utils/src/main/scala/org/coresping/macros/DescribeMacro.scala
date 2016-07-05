package org.coresping.macros

import language.experimental.macros
import scala.reflect.macros.Context

object DescribeMacro {

  /**
   * A macro to generate a string with information about the given context.
   * Useful for logging.
   * {{{
   *   import org.corespring.macros.DescribeMacro.describe
   *
   *   class Foo{
   *     def bar(baz:String) : Unit = {
   *       println(describe(baz))
   *     }
   *   }
   *
   *   new Foo().bar("hi") // prints: function=bar, baz=hi
   * }}}
   * @param params
   * @return
   */
  def describe(params: Any*): String = macro describe_impl

  def describe_impl(c: Context)(params: c.Expr[Any]*): c.Expr[String] = {

    import c.universe._

    val methodName: c.Expr[String] = c.enclosingMethod match {
      case DefDef(_, name, _, _, _, _) =>
        c.literal(name.toString)
      case _ => c.literal("?")
    }

    val trees: c.Expr[Seq[String]] = params.foldRight(reify { Seq.empty[String] }) { (p, acc) =>
      p.tree match {
        // Keeping constants as-is
        // The c.universe prefixes aren't necessary, but otherwise Idea keeps importing weird stuff ...
        case c.universe.Literal(c.universe.Constant(const)) => {
          reify { p.splice.toString +: acc.splice }
        }
        case _ => {
          val paramRep = show(p.tree)
          val paramRepTree = Literal(Constant(paramRep))
          val paramRepExpr = c.Expr[String](paramRepTree)
          reify { s"${paramRepExpr.splice}=${p.splice}" +: acc.splice }
        }
      }
    }

    reify { s"function=${methodName.splice}, ${trees.splice.mkString(", ")}" }
  }
}
