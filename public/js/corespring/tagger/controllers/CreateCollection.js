 angular.module('tagger').directive('contenteditable', function(Collection){ return {
    restrict: 'A',
    scope: {collectionId: '@collectionId', collectionName: '=collectionName'},
    link: function(scope,element,attrs){

      element.html(scope.collectionName)

      // view -> model
      element.bind('blur', function() {

        Collection.update({id: scope.collectionId},{name: element.html()},function(data){
            scope.collectionName = data.name
        },function(err){
            console.log("error update collection name: "+err)
            element.html(scope.collectionName)
        })
      });

      element.bind('keyup', function(evt){
        if(evt.which == 13){  //enter key was pressed
            element.html(element.context.innerText);
            element.blur();
        }
      })

//      // model -> view
//      ctrl.$render = function() {
//        element.html(ctrl.$viewValue);
//      };
//
//      // load init value from DOM
//      ctrl.$setViewValue(element.html());

    }
 }})
 //controller
 angular.module('tagger').controller("CreateCollection",['$scope', '$rootScope', 'Collection',
 function($scope, $rootScope, Collection){
   //this looks kind of weird. this is all for opening and closing the collections modal correctly between MainNavController and this controller
   $rootScope.$watch('collectionsWindowRoot',function(){
     $scope.collectionsWindow = $rootScope.collectionsWindowRoot
   })
   $scope.closeCollectionWindow = function(){
     $scope.collectionsWindow = false;
   }
   $scope.$watch('collectionsWindow',function(){
    if(!$scope.collectionsWindow && $rootScope.collectionsWindowRoot) $rootScope.collectionsWindowRoot = false;
    else if($scope.collectionsWindow && $rootScope.collections){  //if dialog is shown, populate collections table with collections that org has write access too
        $scope.collections = _.filter($rootScope.collections, function(c){
            return (c.access & 3) == 3
        })
    }
   })
   $scope.createCollection = function(collectionName){
     if(collectionName){
       Collection.create({},{name:collectionName},function(data){
           $rootScope.collections.push(data);
           $scope.collections.push(data);
           $scope.searchParams.collection.push(data)
           $('#newcollection').val('');
       },function(err){
           console.log("create collection: error: " + err);
       })
     }
   };
   $scope.deleteCollection = function(collId){
     Collection.remove({id: collId},function(data){
        //todo: add success message
        $rootScope.collections = _.filter($rootScope.collections, function(c){
            return c.id !== collId
        })
        $scope.collections = _.filter($scope.collections, function(c){
            return c.id !== collId
        })
        if($scope.searchParams.collection){
          $scope.searchParams.collection = _.filter($scope.searchParams.collection, function(searchcoll){
              var found = _.find($rootScope.collections, function(c){
                return c.id == searchcoll.id;
              });
              return found;
          });
        }
        console.log("successfully deleted")
     },function(err){
        //todo: add error message
        console.log("error deleting collection: "+err)
     })
   }
 }])