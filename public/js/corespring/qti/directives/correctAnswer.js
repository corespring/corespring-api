'use strict';
//used to display correct answer modal
//set the correctAnswerBody to html that should be displayed for the answer
angular.module('qti.directives').directive('correctanswer', ['$compile', function($compile){
  return {
    restrict: 'E',
    template: [
    "<div>",
    "<a href='' ng-click='showCorrectAnswer=true' ng-show='incorrectResponse' ng-transclude></a>",
    "<div ui-modal ng-model='showCorrectAnswer' close='showCorrectAnswer=false'>",
    "<div class='modal-header'>",
    "<button type='button' class='close' ng-click='showCorrectAnswer=false'>&#215;</button>",
    "<h3 id='myModalLabel'>The Correct Answer</h3>",
    "</div>",
    "<div class='modal-body'></div>",
    "<div class='modal-footer' style='text-align: left;'><a href='' ng-click='showCorrectAnswer=false'>See your answer</a></div>",
    "</div>",
    "</div>"
    ].join("\n"),
    scope: {correctAnswerBody: '='},
    transclude: true,
    link: function(scope, element, attrs) {
      scope.$watch("correctAnswerBody",function(){
        if(scope.correctAnswerBody){
          if(scope.correctAnswerBody === "clear"){
            scope.incorrectResponse = false
          } else {
            scope.incorrectResponse = true;
            element.find('.modal-body').html(scope.correctAnswerBody)
            $compile(element.find('.modal-body'))(scope)
          }
        }
      })
    }
  }
}]);
