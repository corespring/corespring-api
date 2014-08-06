package org.corespring.qtiToV2.customScoring

import org.corespring.container.js.rhino.LocalErrorReporter
import org.mozilla.javascript.{ Context, Scriptable }
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsObject, Json }

class CustomScoringTransformerTest extends Specification with JsContext {

  "CustomScoring" should {
    "wrap the js" in {
      val transformer = new CustomScoringTransformer()
      transformer.generate("//qti-js", Map()) === transformer.template("//qti-js", Map())
    }

    "executes the js" in {

      val qtiJs =
        """
          |var correctAnswers = 0;
          |  if (RESPONSE.value.indexOf("1") != -1) correctAnswers += 1;
          |  if (RESPONSE.value.indexOf("2") != -1) correctAnswers += 1;
          |  if (RESPONSE.value.indexOf("3") != -1) correctAnswers += 1;
          |
          |  var score = 0;
          |  if (correctAnswers == 1) score = 0.5
          |  if (correctAnswers == 2) score = 0.8
          |  if (correctAnswers == 3) score = 1.0
          |
          |  var outcome = {};
          |  outcome.score = score;
          |  outcome;
        """
          .stripMargin
      val transformer = new CustomScoringTransformer()
      val js = transformer.generate(qtiJs, Map())

      val answers = Json.obj()

      val result = executeJs(js, answers)

      result === Json.obj()

    }

  }

  private def executeJs(js: String, answers: JsObject): Either[Throwable, JsObject] = {

    var wrapped =
      s"""
         |var exports = {};
         |$js
         |
       """.stripMargin
    withJsContext(wrapped) { (context: Context, s: Scriptable) =>
      Right(Json.obj())
    }
  }

}

import org.mozilla.javascript.tools.shell.Global
import org.mozilla.javascript.{ Function => RhinoFunction, _ }

trait JsContext {

  def withJsContext[A](src: String)(f: (Context, Scriptable) => Either[Throwable, A]): Either[Throwable, A] = {
    val ctx = Context.enter()
    ctx.setErrorReporter(new LocalErrorReporter)
    ctx.setOptimizationLevel(-1)
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    try {

      def addSrcToContext(name: String, src: String) = {
        println(s"add  $name to context")
        ctx.evaluateString(scope, src, name, 1, null)
      }

      addSrcToContext("test", src)
      f(ctx, scope)
    } catch {
      case e: RhinoException => Left(e)
      case e: Throwable => Left(e)
    } finally {
      Context.exit()
    }
  }

}
