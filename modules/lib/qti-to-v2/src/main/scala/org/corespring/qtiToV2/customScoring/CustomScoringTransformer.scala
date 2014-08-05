package org.corespring.qtiToV2.customScoring

import play.api.libs.json.JsObject

class CustomScoringTransformer {

  def generate(qtiJs: String, components: Map[String, JsObject]): String = {
    template(qtiJs, components)
  }

  def toLocalVar(key: String, config: JsObject): String = {

    s"""
var $key = toResponseProcessingModel(session.components.$key, '${(config \ "componentType").as[String]}');

     """
  }

  def template(qtiJs: String, components: Map[String, JsObject]): String = {
    s"""

var componentTypeFunctions = {
   'corespring-multiple-choice' : function(answers){
     return {
       value: answers ? answers : []
     }
   }
};

function toResponseProcessingModel(answer, componentType){
  var fn = componentTypeFunctions[componentType];

  if(!fn){
    throw new Error('Can\'t find mapping function for ' + componentType);
  }
  return fn(answer);
}

exports.process = function(item, session){

  ${components.map(t => toLocalVar(t._1, t._2)).mkString("\n")}

  /// ----------- this is qti js - can't edit
  $qtiJs
  /// -------------- end qti js
  return {
    components: {},
    summary: {
      percentage: (outcome.score * 100),
      note: 'Overridden score'
    }
  };
};
"""
  }
}
