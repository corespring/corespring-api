package org.corespring.qtiToV2.customScoring

import org.mozilla.javascript.Context

object HasSyntaxErrors {
  import org.mozilla.javascript.{ CompilerEnvirons, Parser }

  def apply(js: String): Either[CustomTransformException, String] = {
    try {
      val cx: Context = Context.enter()
      cx.setLanguageVersion(Context.VERSION_1_5)
      val compilerEnv: CompilerEnvirons = new CompilerEnvirons()
      compilerEnv.initFromContext(cx)
      val parser = new Parser(compilerEnv)
      parser.parse(js, "?", 0)
      Right(js)
    } catch {
      case e: Throwable => Left(new CustomTransformException("Error parsing js", e))
    } finally {
      /** We must exit the context otherwise we risk deadlock */
      Context.exit()
    }
  }
}

