'use strict';

/* Directives */


angular.module('tagger.directives', []).
    directive('appVersion', ['version', function (version) {
    return function (scope, elm, attrs) {
        elm.text(version);
    };
}]);


angular.module('tagger.directives', [])
    .directive('testPlayer', function () {
        return {

            link:function (scope, element, attrs) {
                scope.testPlayer = new TestPlayer();
                var xmlDataName = attrs["testPlayerXml"];

                scope.$watch(xmlDataName, function (newXml) {
                    if (newXml != null && newXml != undefined) {
                        scope.testPlayer.play(newXml);
                    }
                });
            }
        };

    });
