package org.corespring.container.client.processing

import com.google.javascript.jscomp.{ JSSourceFile, CompilerOptions }

trait JsMinifier {

  val defaultOptions: CompilerOptions = {
    val o = new CompilerOptions()
    o.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5)
    o
  }

  def minifyJs(source: String, compilerOptions: Option[CompilerOptions] = None): Either[String, String] = {

    val compiler = new com.google.javascript.jscomp.Compiler()
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val options = compilerOptions.getOrElse(defaultOptions)
    val input = JSSourceFile.fromCode("generated", source)

    compiler.compile(extern, input, options).success match {
      case true => Right(compiler.toSource())
      case false => {
        Left(s"JS Errors: \n${compiler.getErrors.map(e => s"${e.description}: ${compiler.getSourceLine("generated", e.lineNumber)}").mkString("\n")}\n")
      }
    }
  }
}
