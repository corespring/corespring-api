package org.corespring.qtiToV2.customScoring

import org.mozilla.javascript.{ Context, Scriptable }
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsString, JsValue, JsObject, Json }

class CustomScoringTransformerTest extends Specification with JsContext with JsFunctionCalling {

  "CustomScoring" should {
    "wrap the js" in {
      val transformer = new CustomScoringTransformer()
      transformer.generate("//qti-js", Map()) === transformer.template("//qti-js", Map())
    }

    "executes the js" in {

      val qtiJs =
        """
          |var correctAnswers = 0;
          |if (RESPONSE.value.indexOf("1") != -1) correctAnswers += 1;
          |if (RESPONSE.value.indexOf("2") != -1) correctAnswers += 1;
          |if (RESPONSE.value.indexOf("3") != -1) correctAnswers += 1;
          |
          |var score = 0;
          |if (correctAnswers == 1) score = 0.5
          |if (correctAnswers == 2) score = 0.8
          |if (correctAnswers == 3) score = 1.0
          |
          |var outcome = {};
          |outcome.score = score;
          |outcome;
        """
          .stripMargin
      val transformer = new CustomScoringTransformer()

      val answers = Map(
        "RESPONSE" -> Json.obj(
          "componentType" -> "corespring-multiple-choice",
          "answers" -> Json.arr("1", "2", "3")))

      val js = transformer.generate(qtiJs, answers)

      val result = executeJs(js, Json.obj("components" -> answers))

      result === Right(Json.parse("""{"components":{},"summary":{"percentage":100,"note":"Overridden score"}}"""))

    }

  }

  import org.mozilla.javascript.{ Context, Scriptable, Function => RhinoFunction }

  private def executeJs(js: String, answers: JsObject): Either[Throwable, JsObject] = {

    val wrapped =
      s"""
         |var exports = {};
         |$js
         |
       """.stripMargin

    def getProcess(ctx: Context, scope: Scriptable): RhinoFunction = {
      val exports = scope.get("exports", scope).asInstanceOf[Scriptable]
      exports.get("process", exports).asInstanceOf[RhinoFunction]
    }

    try {

      println("--")
      println(wrapped)
      println("--")
      withJsContext(wrapped) { (context: Context, scope: Scriptable) =>
        val process = getProcess(context, scope)
        val result = callJsFunction(wrapped, process, process.getParentScope, Array(Json.obj(), answers))(context, scope)
        println(result)
        result
      }
    } catch {
      case e: Throwable => Left(e)
    }
  }

}

import org.mozilla.javascript.tools.shell.Global
import org.mozilla.javascript.{ Function => RhinoFunction, _ }

class TestErrorReporter extends ErrorReporter {
  override def warning(s: String, s1: String, i: Int, s2: String, i1: Int): Unit = println(s)

  override def error(s: String, s1: String, i: Int, s2: String, i1: Int): Unit = println(s)

  override def runtimeError(s: String, s1: String, i: Int, s2: String, i1: Int): EvaluatorException = {
    println(s)
    new EvaluatorException(s)
  }
}

class TestConsole() {
  def log(msg: String): Unit = println(msg)
}

trait JsContext {

  def withJsContext[A](src: String)(f: (Context, Scriptable) => Either[Throwable, A]): Either[Throwable, A] = {
    val ctx = Context.enter()
    ctx.setErrorReporter(new TestErrorReporter())
    ctx.setOptimizationLevel(-1)
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    def addToScope(name: String)(thing: Any) = ScriptableObject.putProperty(scope, name, thing)

    addToScope("console")(new TestConsole())
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

trait JsFunctionCalling {

  def jsObject(json: JsValue)(implicit ctx: Context, scope: Scriptable): AnyRef = {
    val jsonString = Json.stringify(json)
    json match {
      case s: JsString => Context.javaToJS(s.value, scope)
      case _ => toObject.call(ctx, scope, scope, Array(jsonString))
    }
  }

  def jsJson(implicit scope: Scriptable) = scope.get("JSON", scope).asInstanceOf[ScriptableObject]

  def toObject(implicit scope: Scriptable): RhinoFunction = jsJson.get("parse", jsJson).asInstanceOf[RhinoFunction]

  def toJsonString(implicit scope: Scriptable): RhinoFunction = jsJson.get("stringify", jsJson).asInstanceOf[RhinoFunction]

  def callJsFunction(rawJs: String, fn: RhinoFunction, parentScope: Scriptable, args: Array[JsValue])(implicit ctx: Context, rootScope: Scriptable): Either[Throwable, JsObject] = {
    try {
      val jsArgs: Array[AnyRef] = args.toArray.map(jsObject(_))
      val result = fn.call(ctx, rootScope, parentScope, jsArgs)
      val jsonString: Any = toJsonString.call(ctx, rootScope, rootScope, Array(result))
      val jsonOut = Json.parse(jsonString.toString)
      Right(jsonOut.asInstanceOf[JsObject])
    } catch {
      case e: EcmaError => Left(e)
      case e: Throwable => Left(new RuntimeException("General error while processing js", e))
    }
  }
}
