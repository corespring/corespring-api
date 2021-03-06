/**
 * Will be included for items that are being rendered in print mode and contain choiceInteraction
 */
angular.module('qti.directives').directive('simplechoice', function(QtiUtils){

  return {
    restrict: 'ECA',
    replace: true,
    scope: true,
    transclude: true,
    compile: function(tElement, tAttrs, transclude){
      // determine input type by inspecting markup before modifying DOM
      var style = 'print-choice';
      var choiceInteractionElem = tElement.parent();
      var maxChoices = choiceInteractionElem.attr('maxChoices');

      if (maxChoices == 1) {
        style += ' print-choice-single';
      }

      var template =  '<div class="' + style + '">&nbsp;</div><div ng-transclude class="choice-content"></div>';

      // now can modify DOM
      tElement.html(template);

      // return link function
      return function(localScope, element, attrs) {

      };
    }
  };
});
