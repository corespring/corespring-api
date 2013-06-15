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
 angular.module('tagger').controller("CreateCollection",['$scope', '$rootScope',
 function($scope, $rootScope){
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
           if($rootScope.collections) $rootScope.collections.push(data)
       },function(err){
           console.log("create collection: error: " + err);
       })
     }
   };
 }])