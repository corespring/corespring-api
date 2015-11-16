package org.corespring.qtiToV2.customScoring

import java.io.File

import org.apache.commons.io.FileUtils
import org.mozilla.javascript.{ Context, Scriptable }
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsString, JsValue, JsObject, Json }

case class TestSet(
  name: String,
  qti: String,
  session: JsObject,
  outcomes: JsObject,
  typeMap: Map[String, String],
  expected: JsValue,
  exceptionExpected: Boolean)

class CustomScoringTransformerTest extends Specification with JsContext with JsFunctionCalling {

  private def jsExecutionWorks(s: TestSet): Result = {
    val answers = (s.session \ "components").as[Map[String, JsObject]]
    val transformer = new CustomScoringTransformer()
    transformer.generate(s.qti, answers, s.typeMap) match {
      case Left(CustomTransformException(msg, t)) => {
        if (s.exceptionExpected) success else {
          t.printStackTrace()
          failure(s"Bad js: $msg")
        }
      }
      case Right(js) => {
        if (s.exceptionExpected) {
          failure(s"Expected this js to fail - but it didn't: ${s.name}")
        } else {
          val result = executeJs(js, s.session, s.outcomes)
          result === Right(s.expected)
        }
      }
    }
  }

  trait BaseScope extends Scope {
    def sets: Seq[TestSet]
  }

  class TestSetScope(val sets: Seq[TestSet]) extends BaseScope

  "CustomScoring" should {
    "wrap the js" in {
      val transformer = new CustomScoringTransformer()
      transformer.generate("//qti-js", Map(), Map()) === transformer.generate("//qti-js", Map(), Map())
    }
  }

  "multiple-choice" should {
    val sets = loadFileSets("corespring-multiple-choice")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "drag-and-drop" should {
    val sets = loadFileSets("corespring-drag-and-drop")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "text-entry" should {
    val sets = loadFileSets("corespring-text-entry")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "inline-choice" should {
    val sets = loadFileSets("corespring-inline-choice")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "line" should {
    val sets = loadFileSets("corespring-line")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "function-entry" should {
    val sets = loadFileSets("corespring-function-entry")
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  "bad js" should {
    val sets = loadFileSets("badJs", "qti.js", FileUtils.readFileToString)
    examplesBlock {
      sets.map { s => s"execute the js + session for: ${s.name}" >> jsExecutionWorks(s) }
    }
  }

  def xmlFileToJs(f: File): String = {
    try {
      val xml = scala.xml.XML.loadFile(f)
      (xml \ "responseProcessing").text
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RuntimeException("Can't parse file: ", e)
      }
    }
  }
  val QtiXml = "qti.xml"

  def loadFileSets(dir: String, fileName: String = QtiXml, fn: File => String = xmlFileToJs): Seq[TestSet] = {
    val url = this.getClass.getResource(dir)
    require(url != null, "The url is null")

    val file = new File(url.toURI)

    def mkSet(qti: File, s: File): TestSet = {

      val json = try {
        Json.parse(FileUtils.readFileToString(s))
      } catch {
        case e: Throwable => throw new RuntimeException(s"Error parsing: ${s.getAbsolutePath}")
      }

      val item = (json \ "item" \ "components").as[Map[String, JsObject]].map { (t: (String, JsObject)) =>
        (t._1 -> ((t._2) \ "componentType").as[String])
      }

      TestSet(
        s"${s.getParentFile.getName}/${s.getName}",
        qti = fn(qti), //js,
        (json \ "session").as[JsObject],
        (json \ "outcomes").asOpt[JsObject].getOrElse(Json.obj()),
        item,
        (json \ "expected").asOpt[JsObject].getOrElse(Json.obj()),
        (json \ "exceptionExpected").asOpt[Boolean].getOrElse(false))
    }

    /**
     * A dir containing 1 qti.js and n session.json files
     * @param dir
     * @return
     */
    def setList(dir: File): Seq[TestSet] = {
      val QtiFile = fileName //"qti.xml"
      val split = dir.listFiles.toSeq.groupBy(f => if (f.getName == QtiFile) QtiFile else "sessions")

      require(split.get(QtiFile).isDefined && split.get(QtiFile).get.headOption.isDefined, s"no $QtiFile in dir: ${dir.getAbsolutePath} ")

      val out = for {
        qtiSeq <- split.get(QtiFile)
        qti <- qtiSeq.headOption
        sessions <- split.get("sessions")
      } yield {
        for (s <- sessions.filter(_.getName.endsWith("json"))) yield mkSet(qti, s)
      }

      out.getOrElse {
        throw new RuntimeException(s"Error loading qti + sessions from: ${dir.getAbsolutePath}")
      }
    }

    lazy val sets: Seq[TestSet] = {
      if (file.exists) {
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

  private def executeJs(js: String, answers: JsObject, outcomes: JsObject): Either[Throwable, JsObject] = {

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
        val result = callJsFunction(wrapped, process, process.getParentScope, Array(Json.obj(), answers, outcomes))(context, scope)
        println(result)
        result
      }
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        println(e.getMessage)
        Left(e)
      }
    }
  }

}

import org.mozilla.javascript.tools.shell.Global
import org.mozilla.javascript.{ Function => RhinoFunction, _ }

class TestErrorReporter extends ErrorReporter {
  override def warning(s: String, s1: String, i: Int, s2: String, i1: Int): Unit = println(s)

  override def error(s: String, s1: String, i: Int, s2: String, i1: Int): Unit = println(s)

  override def runtimeError(s: String, s1: String, i: Int, s2: String, i1: Int): EvaluatorException = {
    println("---------> runtimeError")
    println(s)
    println(s1)
    println(s2)
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
        ctx.evaluateString(scope, src, name, 1, null)
      }

      addSrcToContext("test", src)
      f(ctx, scope)
    } catch {
      case e: RhinoException => {
        Left(e)
      }
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
    def logError(e: Throwable): Unit = {
      println("----------------- failing js")
      val out: String = rawJs.lines.toSeq.zipWithIndex.map { t => s"${t._2}: ${t._1}" }.mkString("\n")
      println(out)
      println("--")
      e.printStackTrace()
    }

    try {
      val jsArgs: Array[AnyRef] = args.toArray.map(jsObject(_))
      val result = fn.call(ctx, rootScope, parentScope, jsArgs)
      val jsonString: Any = toJsonString.call(ctx, rootScope, rootScope, Array(result))
      val jsonOut = Json.parse(jsonString.toString)
      Right(jsonOut.asInstanceOf[JsObject])
    } catch {
      case e: EcmaError => {
        logError(e)
        println(e.details())
        Left(e)
      }
      case e: Throwable => {
        logError(e)
        Left(new RuntimeException("General error while processing js", e))
      }
    }
  }
}