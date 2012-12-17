function loadModule(name){
  try {
    return angular.module(name);
  }
  catch (e){
    return angular.module(name, []);
  }
}

var module = loadModule('corespring-directives');

/**
 * A mod of the tagger result pager.
 * It delegates out more if its functionality to allow for reusability.
 */
module.directive("resultPager", function(){

  return {
    link: function($scope, $element, $attrs){
      console.log("lti result pager");

      var listModel = $attrs['listModel'];
      var currentItem = $attrs['currentItem'];
      var loadItemFn = $attrs['loadItem'];
      var loadMoreFn = $attrs['loadMore'];
      var itemCount = $attrs['itemCount'];

      var totalNoOfItems = 0;

      $scope.$watch(itemCount, function(newItemCount){
        console.log("totalNoOfItems: " + newItemCount);
        totalNoOfItems = newItemCount;
        updatePageText(0);
      });

      $scope.$watch(currentItem, function(newCurrentItem){
        var index = getIndexByKey($scope[listModel], "id", $scope[currentItem]);
        updatePageText(index);
      });

      var updatePageText = function(index){
        $scope.pagerText = (index + 1) + " of " + totalNoOfItems;
      };

      var uid = ($attrs['uid'] || "id");

      $scope.next = function () {
        changePage(1);
      };

      $scope.previous = function () {
        changePage(-1);
      };

      function getIndexByKey(items, key, obj){

          if(!items){
            return -1;
          }

          for(var i = 0 ; i < items.length; i++){
             if(items[i][key] == obj[key]){
               return i;
             }
          }
          return -1;
        }


      function changePage(number) {

        var currentIndex = getIndexByKey($scope[listModel], "id", $scope[currentItem]);


        var proposedIndex = currentIndex + number;

        if(proposedIndex < 0){
          console.log("currentIndex is < 0");
          return;
        }

        if(proposedIndex > totalNoOfItems -1 ){
          console.log("currentIndex is > than totalNoOfItems");
          return;
        }

        updatePageText(proposedIndex);

        console.log("proposedIndex: "  + proposedIndex);

        var nextItem = $scope[listModel][proposedIndex];
        if (nextItem) {
          $scope[loadItemFn](nextItem.id);
        } else {
          
          var onMoreLoaded = function(){
            var loadedNextItem = $scope[listModel][proposedIndex];
            
            if(!loadedNextItem){
              throw "No more items loaded";
            }

            $scope[loadItemFn](loadedNextItem.id);
          };

          $scope[loadMoreFn](proposedIndex, onMoreLoaded);
        }
      }

    }
  };

});

