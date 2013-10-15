'use strict';
angular.module('qti.directives').directive('tex', function () {
  return {
    restrict: 'E',
    compile: function(element, attrs) {
      var content = element.html();
      if (attrs.inline == "false")
        element.html("$$"+content+"$$");
      else
        element.html("\\("+content+"\\)");
    }
  }
});
