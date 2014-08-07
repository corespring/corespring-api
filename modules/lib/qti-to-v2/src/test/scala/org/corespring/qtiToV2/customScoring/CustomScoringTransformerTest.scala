package org.corespring.qtiToV2.customScoring

import java.io.File

import org.mozilla.javascript.{ Context, Scriptable }
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsString, JsValue, JsObject, Json }

case class TestSet(name: String, qti: String, session: JsObject, expected: JsValue)

class CustomScoringTransformerTest extends Specification with JsContext with JsFunctionCalling {

  private def jsExecutionWorks(s: TestSet): Result = {
    val answers = (s.session \ "components").as[Map[String, JsObject]]
    val transformer = new CustomScoringTransformer()
    val js = transformer.generate(s.qti, answers)
    val result = executeJs(js, s.session)
    result === Right(s.expected)
  }

  trait BaseScope extends Scope {
    def sets: Seq[TestSet]
  }

  class TestSetScope(val sets: Seq[TestSet]) extends BaseScope

  "CustomScoring" should {
    "wrap the js" in {
      val transformer = new CustomScoringTransformer()
      transformer.generate("//qti-js", Map()) === transformer.template("//qti-js", Map())
    }
  }

  "variations" should {
    val sets = loadFileSets("corespring-multiple-choice")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  def loadFileSets(dir: String): Seq[TestSet] = {
    val url = this.getClass.getResource(dir)
    require(url != null, "The url is null")

    val file = new File(url.toURI)

    def mkSet(qti: File, s: File): TestSet = {
      val json = Json.parse(scala.io.Source.fromFile(s).getLines().mkString("\n"))
      TestSet(
        s"${s.getParentFile.getName}/${s.getName}",
        qti = scala.io.Source.fromFile(qti).getLines.mkString("\n"),
        (json \ "session").as[JsObject],
        (json \ "expected").as[JsObject])
    }

    /**
     * A dir containing 1 qti.js and n session.json files
     * @param dir
     * @return
     */
    def setList(dir: File): Seq[TestSet] = {
      val split = dir.listFiles.toSeq.groupBy(f => if (f.getName == "qti.js") "qti.js" else "sessions")

      require(split.get("qti.js").isDefined && split.get("qti.js").get.headOption.isDefined, s"no qti in dir: ${dir.getAbsolutePath} ")

      val out = for {
        qtiSeq <- split.get("qti.js")
        qti <- qtiSeq.headOption
        sessions <- split.get("sessions")
      } yield {
        for (s <- sessions) yield mkSet(qti, s)
      }

      out.getOrElse {
        throw new RuntimeException(s"Error loading qti + sessions from: ${dir.getAbsolutePath}")
      }
    }

    lazy val sets: Seq[TestSet] = {
      if (file.exists) {
        println(url)
        println(file)
        println(file.exists)
        val out: Seq[Seq[TestSet]] = file.listFiles.toSeq.map(setList)
        val o: Seq[TestSet] = out.flatten
        o
      } else {
        println(s"Error - missing dir: $dir")
        Seq.empty
      }
    }
    sets
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
