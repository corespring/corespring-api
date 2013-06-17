 angular.module('tagger').directive('contenteditable', function(Collection){ return {
    restrict: 'A',
    scope: {collectionId: '@collectionId', collectionName: '=collectionName', newAlert: '=newAlert', createSortedCollection: '=createSortedCollection'},
    link: function(scope,element,attrs){

      element.html(scope.collectionName)

      // view -> model
      element.bind('blur', function() {
        if(scope.collectionName != element.html()){
          Collection.update({id: scope.collectionId},{name: element.html()},function(data){
              scope.collectionName = data.name
              scope.newAlert("alert alert-success", "Successfully edited collection name");
              scope.createSortedCollection();
          },function(err){
              scope.newAlert("alert alert-error", "Error editing collection name");
              console.log("error update collection name: "+err);
              element.html(scope.collectionName);
          })
        }
      });

      element.bind('keyup', function(evt){
        if(evt.which == 13){  //enter key was pressed
            element.html(element.context.innerText);
            element.blur();
        }
      })

    }
 }})