/**
 * Shared function for handling feedback blocks
 * @return {Object}
 */
var feedbackDirectiveFunction = function ($compile, QtiUtils) {

    return {
        restrict:'ACE',
        template:'<span class="{{cssClass}}"></span>',
        scope:true,
        require:'^assessmentitem',
        link:function (scope, element, attrs) {

            var csFeedbackId = attrs["csfeedbackid"];

            scope.$on('resetUI', function (event) {
                scope.feedback = "";
            });

            scope.cssClass = element[0].localName;

            scope.$watch('itemSession.sessionData.correctResponses', function (responses) {

                if(!responses || !scope.isFeedbackEnabled()) return;

                var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
                scope.feedback = ( feedback || "" );

                element.html(feedback || "")
                $compile(element.contents())(scope)

              setTimeout(function () {
                if (typeof(MathJax) != "undefined") {
                  MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                }
              }, 10);

            });
            scope.feedback = "";
        },
        controller:function ($scope) {
            this.scope = $scope;
        }
    }
};
angular.module('qti.directives').directive('correctanswer', ['$compile', function($compile){
    return {
        restrict: 'E',
        template: [
            "<a href='' ng-click='showCorrectAnswerModal()' ng-show='incorrectResponse' ng-transclude></a>",
            "<div id='myModal' class='modal hide fade' tabindex='-1' role='dialog' aria-labelledby='myModalLabel' aria-hidden='true'>",
              "<div class='modal-header'>",
                "<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>×</button>",
                "<h3 id='myModalLabel'>Modal header</h3>",
              "</div>",
              "<div class='modal-body'></div>",
              "<div class='modal-footer'>",
                "<button class='btn' data-dismiss='modal' aria-hidden='true'>Close</button>",
                "<button class='btn btn-primary'>Save changes</button>",
              "</div>",
            "</div>",
        ].join("\n"),
        require: '^assessmentitem',
        transclude: true,
        link: function(scope, element, attrs, AssessmentItemCtrl) {
            scope.incorrectResponse = true
            scope.$on("formSubmitted",function(){
               var response = _.find(scope.itemSession.responses,function(r){
                   return r.id === attrs.responseidentifier;
               });
               scope.incorrectResponse = response && !response.outcome.isCorrect
            });
            scope.showCorrectAnswerModal = function(){
                var htmlResponse = _.find(scope.correctHtmlResponses, function(html,key){
                    return key == attrs.responseidentifier;
                })
                if(htmlResponse){
                   element.find('.modal-body').html(htmlResponse)
                   $compile(element.find('.modal-body'))(scope)
                   element.find('#myModal').modal({
                      backdropFade: true,
                      dialogFade:true
                    })
                } else {
                    console.error("no html correct response found to be displayed in modal")
                }
            }
        }
    }
}])

angular.module('qti.directives').directive('feedbackblock', ['$compile', feedbackDirectiveFunction]);

angular.module('qti.directives').directive('feedbackinline', ['$compile', feedbackDirectiveFunction]);

angular.module('qti.directives').directive('modalfeedback', ['$compile', feedbackDirectiveFunction]);
