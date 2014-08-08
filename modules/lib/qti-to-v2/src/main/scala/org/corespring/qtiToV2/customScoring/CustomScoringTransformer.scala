package org.corespring.qtiToV2.customScoring

import play.api.libs.json.JsObject

case class CustomTransformException(msg: String, t: Throwable = null) extends RuntimeException(msg, t)

class CustomScoringTransformer {

  def generate(qtiJs: String, session: Map[String, JsObject], typeMap: Map[String, String]): Either[CustomTransformException, String] = synchronized {
    HasSyntaxErrors(qtiJs) match {
      case Left(e) => Left(e)
      case Right(js) => Right(wrapJs(js, session, typeMap))
    }
  }

  private def getType(key: String, m: Map[String, String]) = m.getOrElse(key, "unknown-type")

  private def toLocalVar(key: String, config: JsObject, componentType: String): String = {
    s"""var $key = toResponseProcessingModel(session.components.$key, '$componentType', outcomes.components.$key || {});"""
  }

  private def wrapJs(js: String, session: Map[String, JsObject], typeMap: Map[String, String]): String = {
    s"""

var mkValue = function(defaultValue){
  return function(comp, outcome){
    return {
      value: comp && comp.answers ? comp.answers : defaultValue,
      outcome: {
        isCorrect: outcome.correctness === 'correct'
      }
    };
  }
};

var toCommaString = function(xy){
  return xy.x + ',' + xy.y;
}

var lineToValue = function(comp, outcome){

  if(comp && comp.answers){
    return {
      value: [ toCommaString(comp.answers.A), toCommaString(comp.answers.B) ],
      outcome: {
        isCorrect: outcome.correctness === 'correct'
      }
    }
  } else {
    return {
      value: ['0,0', '0,0'],
      outcome: {
        isCorrect: outcome.correctness === 'correct'
      }
    }
  }
}

var unknownTypeValue = function(comp, outcome){
  return {
    value: '?',
    outcome: {
      isCorrect: false
    }
  }
};

var componentTypeFunctions = {
 'corespring-extended-text-entry' : mkValue('?'),
 'corespring-drag-and-drop' : mkValue([]),
 'corespring-focus-task' : mkValue([]),
 'corespring-inline-choice' : mkValue('?'),
 'corespring-line' : lineToValue,
 'corespring-multiple-choice' : mkValue([]),
 'corespring-ordering' : mkValue([]),
 'corespring-placement-ordering' : mkValue([]),
 'corespring-point-intercept' : mkValue(['0,0', '0,0']),
 'corespring-select-text' : mkValue([]),
 'corespring-text-entry' : mkValue('?'),
 //If all else fails use this:
 'unknown-type' : unknownTypeValue
};

function toResponseProcessingModel(answer, componentType, outcome){
  var fn = componentTypeFunctions[componentType];

  if(!fn){
    throw new Error('Can\\\'t find mapping function for ' + componentType);
  }
  return fn(answer, outcome);
}

/**
 * CustomScoringTransformer
 * This is a generated js file that wraps qti js in a v2 compatible format.
 */
exports.process = function(item, session, outcomes){

  //console.log("---------> session: " + JSON.stringify(session));
  //console.log("---------> outcomes:  " + JSON.stringify(outcomes));

  outcomes = outcomes || { components: {} };
  outcomes.components = outcomes.components || {};

  if(!session || !session.components){
    console.log("Error: session has no components: " + JSON.stringify(session));
    return "";
  }

  ${session.map(t => toLocalVar(t._1, t._2, getType(t._1, typeMap))).mkString("\n")}
  ${session.map(t => s"//console.log( '->' + JSON.stringify(${t._1}) ); ").mkString("\n")}

  /// ----------- this is qti js - can't edit

  try{
    $js
  } catch(e){
    return {
      components: {},
      summary: {
        percentage: 0,
        note: 'Error occurred processing: ' + e
      }
    };
  }

  /// -------------- end qti js
  return {
    components: {},
    summary: {
      percentage: Math.floor(outcome.score * 100),
      note: 'Overridden score'
    }
  };
};
"""
  }
}
