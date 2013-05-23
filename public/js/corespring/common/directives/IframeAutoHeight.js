'use strict';

function loadModule(name) {
  try {
    return angular.module(name);
  }
  catch (e) {
    return angular.module(name, []);
  }
}

var corespringDirectivesModule = loadModule('corespring-directives');

corespringDirectivesModule.directive("iframeAutoHeight", function () {

  return {
    link: function ($scope, $element, $attrs) {
      console.log("Linking ifa");
      $($element).iframeAutoHeight({debug: true});
    }
  };

});

