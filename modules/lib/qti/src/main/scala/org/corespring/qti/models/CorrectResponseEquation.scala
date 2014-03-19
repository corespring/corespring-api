package org.corespring.qti.models

import java.util.regex.{Matcher, Pattern}
import util.Random
import xml.Node
import org.mozilla.javascript.{EvaluatorException, Context}
import org.mozilla.javascript.tools.shell.Global
import scalaz._

class Domain(var include:Seq[(Int,Int)] = Seq(-10 -> 10), var notInclude: Seq[Int] = Seq())
object Domain{
  /**
   * parse the string received from line attribute
   * @param strDomain expects comma separated single numbers and sets, where sets are the included and denoted by min->max
   *                  and single numbers are the exclusion
   *                  example format: "4->10,-10->-4,5"
   *                  In this case, random points will be generated from 4 to 10 and -4 to -10 and will also
   *                  exclude 5 from the possible random points
   * @return Domain
   */
  def apply(strDomain:String):Domain = {
    val domain = new Domain(Seq(),Seq())
    val sets = strDomain.split(",")
    for (set <- sets){
      if (set.contains("->")){
        val minmax = set.split("->")
        domain.include = domain.include :+ (minmax(0).toInt -> minmax(1).toInt)
      } else {
        domain.notInclude = domain.notInclude :+ set.toInt
      }
    }
    domain
  }
}
/**
 * value is limited to the format y=f(x), where f(x) is some continuous (defined at all points) expression only containing the variable x
 */
case class CorrectResponseEquation(value: String,
  var domain: Domain = new Domain(),
  var variables: (String, String) = "x" -> "y",
  var numOfTestPoints: Int = 50,
  var sigfigs: Int = 3) extends CorrectResponse {

  def isCorrect(responseValue: String): Boolean = CorrectResponseEquation.lineEquationMatch(value, responseValue, domain, variables, numOfTestPoints,sigfigs)
  def isValueCorrect(v: String, index: Option[Int]) = CorrectResponseEquation.lineEquationMatch(value, v, domain, variables, numOfTestPoints,sigfigs)
}
object CorrectResponseEquation {
  def apply(node: Node, lineAttr: String): CorrectResponseEquation = {
    if ((node \ "value").size != 1) {
      throw new RuntimeException("Cardinality is set to single but there is not one <value> declared: " + (node \ "value").toString)
    } else {
      //line: vars:x,y range:10 sigfigs:3
      val lineAttrsArray = lineAttr.split(" ")
      val cr = CorrectResponseEquation((node \ "value").text)
      lineAttrsArray.foreach(attr => {
        val kvpair = attr.split(":")
        if (kvpair.length == 2 || kvpair(0).contains("line") || kvpair(0).contains("eqn")){
          kvpair(0) match {
            case "vars" => {
              val vars = kvpair(1).split(",")
              cr.variables = (vars(0),vars(1))
            }
            case "domain" => {
              cr.domain = Domain(kvpair(1))
            }
            case "sigfigs" => {
              cr.sigfigs = kvpair(1).toInt
            }
            case "line" => cr.numOfTestPoints = 5
            case "eqn" => cr.numOfTestPoints = 50
            case _ =>
          }
        } else throw new RuntimeException("line attributes must be of form: vars:[var1,var2,...] testPoints:[Number] sigfigs:[Number]")
      })
      cr
    }
  }
  //generate random Double which is valid for given domain
  private def getRandomValues(numOfTestPoints:Int,domain:Domain):Seq[Double] = {
    var randomValues:Seq[Double] = Seq();
    //divide the number of test points into sections which correspond to each domain set.
    val sections:Int = numOfTestPoints / domain.include.size;
    val sectionSize:Int = numOfTestPoints / sections;
    val remainder:Int = numOfTestPoints % sections;
    for(section <- 0 to sections-1){
      val numOfValues = if (section == sections-1) sectionSize + remainder else sectionSize
      val (min,max) = domain.include(section % domain.include.size)
      for (dc <- 1 to numOfValues) {
        val random = new Random().nextInt(max - min) + min
        randomValues = randomValues :+ random.toDouble
      }
    }
    randomValues = randomValues.filter(rval => !domain.notInclude.exists(num => num == rval))
    randomValues
  }
  /**
   *
   */
  def lineEquationMatch(value: String, responseValue: String,
    domain: Domain = new Domain(), variables: (String, String) = "x" -> "y",
    numOfTestPoints: Int = 50, sigfigs: Int = 4): Boolean = {


    val ctx = Context.enter
    ctx.setOptimizationLevel(-1)
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    def formatExpression(expr: String, variableValues: Seq[(String, Double)]): String = {
      def replaceVar(expr: String, variable: String, num: Double): String = {
        var m:Matcher = null;
        def patternMatch(pattern:String):Boolean = {
          m = Pattern.compile(pattern).matcher(expr)
          m.matches()
        }
        if (patternMatch(".*?([0-9)])" + variable + "([(0-9]).*")) {
          replaceVar(expr.replaceFirst("[0-9)]" + variable + "[(0-9]", m.group(1) + "*(" + num.toString + ")*" + m.group(2)),variable,num)
        }else if (patternMatch(".*?([0-9)])" + variable + ".*")) {
          replaceVar(expr.replaceFirst("[0-9)]" + variable, m.group(1) + "*(" + num.toString + ")"),variable,num)
        }else if (patternMatch(".*?" + variable + "([(0-9]).*")) {
          replaceVar(expr.replaceFirst(variable + "[(0-9]", "(" + num.toString + ")*" + m.group(2)),variable,num)
        } else {
          expr.replaceAll(variable, "(" + num.toString + ")")
        }
      }
      def replaceExponent(expr: String):String = {
        val m = Pattern.compile(".*?((\\(?-?\\d*(?:[\\.\\/]\\d*)?\\)?)\\^(\\(?-?\\d*(?:[\\.\\/]\\d*)?\\)?)).*?").matcher(expr)
        if (m.matches()){
          val one = m.group(1)
          val two = m.group(2)
          val three = m.group(3)
          replaceExponent(expr.replace(m.group(1),s"Math.pow(${m.group(2)},${m.group(3)})"))
        } else {
          expr
        }
      }
      val noWhitespace = expr.replaceAll("\\s", "")
      val exprVarsReplaced = variableValues.foldRight[String](noWhitespace)((variable, acc) => {
        replaceVar(acc, variable._1, variable._2)
      })
      replaceExponent(exprVarsReplaced)
    }
    /**
     * find coordinates on the graph that fall on the line
     */
    def getTestPoints: Validation[String, Array[(Double, Double)]] = {
      val rhs = value.split("=")(1)
      Success(getRandomValues(numOfTestPoints,domain).map(x => {
        try {
          val expr = formatExpression(rhs, Seq(variables._1 -> x))
          val y = ctx.evaluateString(scope, expr, "?", 1, null).toString.toDouble
          (x, y)
        } catch {
          case e: EvaluatorException => {
            e.printStackTrace()
            return Failure("Error processing javascript")
          }
        }
      }).toArray)
    }
    def round(num:Double):Double = {
      val multiplier: Double = scala.math.pow(10, sigfigs)
      scala.math.floor(num * multiplier) / multiplier
    }
    /**
     * evaluate the expression using the javascript engine
     * @param expr
     * @param variableValues
     * @return lowerBound,  center, upperBound
     */
    def evaluate(expr:String,variableValues:Seq[(String,Double)]):(Double,Double,Double) = {
      val formattedExpr= formatExpression(expr, variableValues)
      val num = ctx.evaluateString(scope, formattedExpr, "?", 1, null).toString.toDouble
      val bound:Double = scala.math.pow(10, 0-sigfigs)
      val center = round(num)
      //round upper and lower bounds as a result of some funky things that occur to double's
      (round(center - bound), center, round(center + bound))
    }
    def isEquivalent(lhs:String, rhs:String): Boolean = {
      try {
        getTestPoints match {
          case Success(testPoints) => {
            testPoints.foldRight[Boolean](true)((testPoint, acc) => if (acc) {
              val variableValues = Seq(variables._1 -> testPoint._1, variables._2 -> testPoint._2)
              val (llowerBound,lcenter,lupperBound) = evaluate(lhs,variableValues)
              val (rlowerBound, rcenter, rupperBound) = evaluate(rhs,variableValues)
              //don't compare directly as there are some trailing decimal places that can cause issues.
              // Instead, compare upper and lower bounds
              (lcenter <= rupperBound && lcenter >= rlowerBound) || (rcenter <= lupperBound && rcenter >= llowerBound)
            } else false)
          }
          case _ => false
        }
      } catch {
        case e: javax.script.ScriptException => false
        case e: NumberFormatException => false
      }
    }
    /**
     * compare response equation with value equation. Since there are many possible forms, we generate random points
     */
    val sides = responseValue.split("=")
    if (sides.length == 2) {
      isEquivalent(sides(0),sides(1))
    } else if (sides.length == 1){
      //if there is no equal sign, assume the rhs is the function to evaluate and lhs side is just f(x)
      isEquivalent(variables._2,sides(0))
    } else false
  }
}

