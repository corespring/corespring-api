package org.corespring.qtiToV2.customScoring

import play.api.libs.json.JsObject

class CustomScoringTransformer {

  def generate(qtiJs: String, session: Map[String, JsObject], typeMap: Map[String, String]): String = {
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
 'corespring-multiple-choice' : mkValue([]),
 'corespring-drag-and-drop' : mkValue([]),
 'corespring-text-entry' : mkValue('?'),
 'corespring-inline-choice' : mkValue('?'),
 'corespring-line' : lineToValue,
 'unknown-type' : unknownTypeValue
};

function toResponseProcessingModel(answer, componentType, outcome){
  var fn = componentTypeFunctions[componentType];

  if(!fn){
    throw new Error('Can\\\'t find mapping function for ' + componentType);
  }
  return fn(answer, outcome);
}

exports.process = function(item, session, outcomes){

  console.log("---------> session: " + JSON.stringify(session));
  console.log("---------> outcomes:  " + JSON.stringify(outcomes));

  outcomes = outcomes || { components: {} };
  outcomes.components = outcomes.components || {};

  if(!session || !session.components){
    console.log("Error: session has no components: " + JSON.stringify(session));
    return "";
  }

  ${session.map(t => toLocalVar(t._1, t._2, getType(t._1, typeMap))).mkString("\n")}
  ${session.map(t => s"//console.log( '->' + JSON.stringify(${t._1}) ); ").mkString("\n")}

  /// ----------- this is qti js - can't edit
  $qtiJs
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

  private def getType(key: String, m: Map[String, String]) = m.getOrElse(key, "unknown-type")

  private def toLocalVar(key: String, config: JsObject, componentType: String): String = {
    s"""var $key = toResponseProcessingModel(session.components.$key, '$componentType', outcomes.components.$key || {});"""
  }
}
