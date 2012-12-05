// module for showing search count and paging through search results in editor
taggerApp.directive('resultsPager', function (SearchService) {
    var resultsPagerDirective = {};

    resultsPagerDirective.controller = function ($scope, $element, $attrs, SearchService, $location) {
        $scope.next = function () {
            changePage(1);
        };
        $scope.previous = function () {
            changePage(-1);
        };
        $scope.updatePagerText = function () {

            if (
                SearchService.itemDataCollection === undefined ||
                SearchService.itemDataCollection.length == 0 ||
                typeof(SearchService.itemDataCollection.indexOf) !== "function") {
                $scope.pagerText = "";
                return;
            }

            var currentIndex = SearchService.itemDataCollection.indexOf(SearchService.currentItem);
            var currentIndexText = "";
            if (currentIndex != -1) {
                currentIndexText = (currentIndex + 1) + " of ";
            }
            $scope.pagerText = currentIndexText + $scope.resultCount + " results";
        };

        function changePage(number) {

            var currentIndex = SearchService.itemDataCollection.indexOf(SearchService.currentItem);
            var proposedIndex = currentIndex + number;

            if (proposedIndex < 0 || proposedIndex > (SearchService.resultCount - 1)) {
                console.debug("can't nav to nothing");
                return;
            }

            var nextItem = SearchService.itemDataCollection[proposedIndex];
            if (nextItem) {
                gotoNextItem(nextItem);
            } else {
                SearchService.loadMore(function () {
                    var nextItem = SearchService.itemDataCollection[currentIndex + number];
                    gotoNextItem(nextItem);
                });
            }
        }

        function gotoNextItem(nextItem) {
            $location.path('/edit/' + nextItem.id);
            SearchService.currentItem = nextItem;
            $scope.updatePagerText();
        }

    };

    resultsPagerDirective.link = function ($scope, element, attr, ctrl) {
        console.log("in resultsPagerDirective");
        // broadcast by search service
        $scope.$on('onSearchCountComplete', function (evt, resultCount) {
            $scope.resultCount = resultCount;
            $scope.updatePagerText();
        });
        $scope.$on('onListViewOpened', function (evt, resultCount) {
            console.log('received onListViewOpened');
            $scope.editViewOpen = false;
        });
        $scope.$on('onEditViewOpened', function (evt, resultCount) {
            console.log('received onEditViewOpened');
            $scope.updatePagerText();
            $scope.editViewOpen = true;
        });

        $scope.$on('createNewItem', function (evt) {
            SearchService.resetDataCollection();
            $scope.updatePagerText();
        });

        $scope.$on('itemDeleted', function(evt){
            SearchService.resetDataCollection();
            $scope.updatePagerText();
        })

    };
    return resultsPagerDirective;
});
