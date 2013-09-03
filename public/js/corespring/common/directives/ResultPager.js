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

/**
 * A paging directive - displays the current item selected out of an array of items.
 * @params:
 * @list-model - the array property on the scope that contains the items
 * @load-item - [loadItem(id)]- a function that loads an item
 * @load-more - [loadMore(proposedIndex, onLoadComplete)] - a function that loads more items and callsback to the directive.
 * @item-count - the property on the scope that contains the item count
 * @current-item - the property on the scope that is the current item - this item must be contained within the item-list array.
 *
 * Usage:
 * {{{
 *  <ul id="resultPager"
                  result-pager
                  load-item="goToItem"
                  load-more="loadMore"
                  list-model="items"
                  item-count="resultCount"
                  current-item="itemData"
                  ></ul>
 * }}}
 */

corespringDirectivesModule.directive("resultPager", function () {

  return {
    link: function ($scope, $element, $attrs) {

      $scope.pagerText = null;

      var listModel = $attrs['listModel'];
      var currentItem = $attrs['currentItem'];
      var loadItemFn = $attrs['loadItem'];
      var loadMoreFn = $attrs['loadMore'];
      var itemCount = $attrs['itemCount'];

      var totalNoOfItems = 0;

      $scope.$watch(itemCount, function (newItemCount) {
        totalNoOfItems = newItemCount;
        updatePageText(0);
      });

      $scope.$watch(listModel, function (newValue) {
        updatePageText(0);
      });

      $scope.$watch(currentItem, function (newCurrentItem) {
        var index = getIndexByKey($scope[listModel], "id", $scope[currentItem]);
        updatePageText(index);
      });

      var updatePageText = function (index) {
        if (!totalNoOfItems || totalNoOfItems == 0) {
          $scope.pagerText = null;
        } else if (index == -1) {
          $scope.pagerText = null;
        } else {
          $scope.pagerText = (index + 1) + " of " + totalNoOfItems;
        }
      };

      var uid = ($attrs['uid'] || "id");

      $scope.next = function () {
        changePage(1);
      };

      $scope.previous = function () {
        changePage(-1);
      };

      function getIndexByKey(items, key, obj) {

        if (!items || !obj) {
          return -1;
        }

        for (var i = 0; i < items.length; i++) {
          if (items[i][key] == obj[key]) {
            return i;
          }
        }
        return -1;
      }


      function changePage(number) {

        var currentIndex = getIndexByKey($scope[listModel], "id", $scope[currentItem]);


        var proposedIndex = currentIndex + number;

        if (proposedIndex < 0) {
          return;
        }

        if (proposedIndex > totalNoOfItems - 1) {
          return;
        }

        updatePageText(proposedIndex);


        var nextItem = $scope[listModel][proposedIndex];
        if (nextItem) {
          $scope[loadItemFn](nextItem.id);
        } else {

          var onMoreLoaded = function () {
            var loadedNextItem = $scope[listModel][proposedIndex];

            if (!loadedNextItem) {
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

