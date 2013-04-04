qtiDirectives.directive("extendedtextinteraction", function () {
  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    template: '<table class="open-response-table"><tr><td>Open Response</table>',
    link: function (scope, element, attrs) {
    }

  };
});
