package org.corespring.qtiToV2.customScoring

import play.api.libs.json.JsObject

class CustomScoringTransformer {

  def generate(qtiJs: String, components: Map[String, JsObject]): String = {
    template(qtiJs, components)
  }

  def toLocalVar(key: String, config: JsObject): String = {

    s"""
        if(!session || !session.components){
          console.log("Error: session has no components: " + JSON.stringify(session));
          return "";
        }

var $key = toResponseProcessingModel(session.components.$key, '${(config \ "componentType").as[String]}');

     """
  }

  def template(qtiJs: String, components: Map[String, JsObject]): String = {
    s"""

var mkValue = function(defaultValue){
  return function(comp){
    return {
      value: comp && comp.answers ? comp.answers : defaultValue
    };
  }
};

var toCommaString = function(xy){
  return xy.x + ',' + xy.y;
}

var lineToValue = function(comp){

  if(comp && comp.answers){
    return {
      value: [ toCommaString(comp.answers.A), toCommaString(comp.answers.B) ],
      outcome: {
        isCorrect: false
      }
    }
  } else {
    return {
      value: ['0,0', '0,0'],
      outcome: {
        isCorrect: false
      }
    }
  }
}

var componentTypeFunctions = {
 'corespring-multiple-choice' : mkValue([]),
 'corespring-drag-and-drop' : mkValue([]),
 'corespring-text-entry' : mkValue('?'),
 'corespring-inline-choice' : mkValue('?'),
 'corespring-line' : lineToValue
};

function toResponseProcessingModel(answer, componentType){
  var fn = componentTypeFunctions[componentType];

  if(!fn){
    throw new Error('Can\\\'t find mapping function for ' + componentType);
  }
  return fn(answer);
}

exports.process = function(item, session){

  console.log("---------> session: " + JSON.stringify(session));

  ${components.map(t => toLocalVar(t._1, t._2)).mkString("\n")}
  ${components.map(t => s"//console.log( '->' + JSON.stringify(${t._1}) ); ").mkString("\n")}

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
}
