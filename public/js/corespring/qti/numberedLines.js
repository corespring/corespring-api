qtiDirectives.directive('line', function () {
        return {
            restrict:'E',
            require:'^p',
            transclude:true,
            template:'<span ng-transclude></span><br>'
        }
    }
);

qtiDirectives.directive('p',
    function ($timeout) {

        return {
            restrict:'E',
            transclude:true,
            require:'^numberedLines',
            scope:true,
            template: "<span class=\"numbers\" style=\"padding-left: 20px; padding-right: 10px; width: 20px; margin-left:-30px\" ng-bind-html-unsafe=\"numbersHtml\"></span><div  ng-transclude></div>",
            link: function (scope, elm, attrs, container) {
                    var line = container.getLastLine();
                    var matchedLines = angular.element(elm).html().match(/<br/gi)
                    var numOfLines = (!!matchedLines ? matchedLines.length : 1);
                    var s = '';
                    for (var i = 0; i < numOfLines; i++)
                        s += (++line) + "<br/>";
                    scope.numbersHtml = s;
                    container.setLastLine(line);
                }

            }

        }
    }
)

qtiDirectives.directive('numberedLines', function () {
    return {
        restrict:'C',
        scope:true,
        controller:function ($scope, $element, $attrs) {
            $scope.lastLine = 0;
            this.getLastLine = function () {
                return $scope.lastLine;
            }
            this.setLastLine = function (value) {
                $scope.lastLine = value;
            }
        }
    }
});