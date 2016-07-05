package org.coresping.macros

import language.experimental.macros
import scala.reflect.macros.Context

object DescribeMacro {

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
