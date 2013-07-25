package qti.models

import interactions.TextEntryInteraction
import javax.script.ScriptEngineManager
import xml.Node
import java.util.regex.Pattern
import util.Random

/**
 * value is limited to the format y=f(x), where f(x) is some continuous (defined at all points) expression only containing the variable x
 */
case class CorrectResponseLineEquation(value: String,
                                       range:(Int,Int) = (-10,10),
                                       variables:(String,String) = "x" -> "y",
                                       numOfTestPoints:Int = 10) extends CorrectResponse{

  def isCorrect(responseValue:String):Boolean = CorrectResponseLineEquation.lineEquationMatch(value,responseValue,range,variables,numOfTestPoints)
  def isValueCorrect(v: String, index: Option[Int]) = CorrectResponseLineEquation.lineEquationMatch(value,v,range,variables,numOfTestPoints)
}
object CorrectResponseLineEquation{
  val engine = new ScriptEngineManager().getEngineByName("JavaScript")
  def apply(node:Node, lineAttr:String):CorrectResponseLineEquation = {
    if ((node \ "value").size != 1) {
      throw new RuntimeException("Cardinality is set to single but there is not one <value> declared: " + (node \ "value").toString)
    }
    else {
      var m = Pattern.compile("^line:([a-z]),([a-z]),([0-9]+)$").matcher(lineAttr);
      if(m.matches()){
        CorrectResponseLineEquation(
          (node\"value").text,
          (0-m.group(3).toInt,m.group(3).toInt),
          (m.group(1),m.group(2))
        )
      }else{
        m = Pattern.compile("^line:([a-z]),([a-z])$").matcher(lineAttr)
        if (m.matches()){
          CorrectResponseLineEquation(
            (node\"value").text,
            variables = (m.group(1),m.group(2))
          )
        }else{
          CorrectResponseLineEquation((node \ "value").text)
        }
      }
    }
  }

  /**
   *
   */
  def lineEquationMatch(value:String, responseValue:String,
                        range:(Int,Int) = (-10,10), variables:(String,String) = "x" -> "y",
                        numOfTestPoints:Int = 2):Boolean = {
    def formatExpression(expr:String,variableValues:Seq[(String,Int)]):String = {
      def replaceVar(expr:String, variable:String, num:Int):String = {
        var newExpr = expr
        var m = Pattern.compile(".*([0-9)])"+variable+"([(0-9]).*").matcher(newExpr)
        if (m.matches()){
          newExpr = newExpr.replaceAll("[0-9)]"+variable+"[(0-9]",m.group(1)+"*("+num.toString+")*"+m.group(2))
        }
        m = Pattern.compile(".*([0-9)])"+variable+".*").matcher(newExpr)
        if (m.matches()){
          newExpr = newExpr.replaceAll("[0-9)]"+variable,m.group(1)+"*("+num.toString+")")
        }
        m = Pattern.compile(".*"+variable+"([(0-9]).*").matcher(newExpr)
        if (m.matches()){
          newExpr = newExpr.replaceAll(variable+"[(0-9]","("+num.toString+")*"+m.group(2))
        }
        newExpr = newExpr.replaceAll(variable,"("+num.toString+")")
        newExpr
      }
      val noWhitespace = expr.replaceAll("\\s","")
      variableValues.foldRight[String](noWhitespace)((variable,acc) =>{
        replaceVar(acc,variable._1,variable._2)
      })
    }
    /**
     * find coordinates on the graph that fall on the line
     */
    def getTestPoints:Array[(Int,Int)] = {
      val rhs = value.split("=")(1)
      var testCoords:Array[(Int,Int)] = Array()
      for (i <- 1 to numOfTestPoints){
        val xcoord = new Random().nextInt(range._2 - range._1)+range._1
        val ycoord = engine.eval(formatExpression(rhs,Seq(variables._1 -> xcoord))).toString.toDouble
        testCoords = testCoords :+ (xcoord,ycoord.toInt)
      }
      testCoords
    }
    /**
     * compare response equation with value equation. Since there are many possible forms, we generate random points
     */
    val sides = responseValue.split("=")
    if (sides.length == 2){
      val lhs = sides(0)
      val rhs = sides(1)
      try{
        getTestPoints.foldRight[Boolean](true)((testPoint,acc) => if (acc){
          val variableValues = Seq(variables._1 -> testPoint._1, variables._2 -> testPoint._2)
          val lfe = formatExpression(lhs, variableValues)
          val rfe = formatExpression(rhs, variableValues)
          //replace the x and y vars with the values of testPoint then evaluate the two expressions with the JSengine.
          // the two sides should be equal
          engine.eval(formatExpression(lhs, variableValues)) == engine.eval(formatExpression(rhs, variableValues))
        } else false)
      }catch{
        case e:javax.script.ScriptException => false
        case e:NumberFormatException => false
      }
    } else false
  }
}

