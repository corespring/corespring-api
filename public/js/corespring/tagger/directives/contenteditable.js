angular.module('tagger').directive('contenteditable', function (CollectionManager) {
  "use strict";
  return {
    restrict: 'A',
    scope: {collectionId: '@collectionId', collectionName: '=collectionName', newAlert: '=newAlert', createSortedCollection: '=createSortedCollection'},
    link: function (scope, element, attrs) {

      element.html(scope.collectionName);

      element.bind('blur', function () {
        if (scope.collectionName !== element.html()) {

          var onError = function () {
            scope.newAlert("alert alert-error", "Error editing collection name");
            element.html(scope.collectionName);
          };
          //TODO - name needs to be cleaned up
          var newName = element.html()
            .replace(/\n/g, '')
            .replace(/^\s*/g, '');
          CollectionManager.renameCollection(scope.collectionId, newName, null, onError);
        }
      });

      element.bind('keyup', function (evt) {
        if (evt.which === 13) {  //enter key was pressed
          element.html(element.context.innerText);
          element.blur();
        }
      });

    }
  };
});