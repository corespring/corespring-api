function HomeController($scope, $rootScope, $timeout, $http, $location, AccessToken, ItemService, ServiceLookup, SupportingMaterial, SearchService) {
    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.accessToken = AccessToken;

    $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();

    $scope.search = function() {
        SearchService.search($scope.searchParams, function(res){
            $scope.items = res;
        });
    }

    $scope.loadMore = function () {
        SearchService.loadMore(function () {
                // re-bind the scope collection to the services model after result comes back
                $scope.items = SearchService.itemDataCollection;
                //Trigger MathJax
                setTimeout(function(){
                    MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
                }, 200);

            }
        );
    };




    $scope.showGradeLevel = function () {
        return $scope.createGradeLevelString(this.item.gradeLevel);
    };

    $scope.showItemType = function (item) {

        if (item.itemType != "Other") {
            return item.itemType;
        }
        return item.itemType + ": " + item.itemTypeOther;
    };

    $scope.createGradeLevelString = function (gradeLevels) {

        function sortGradeLevels(a, b) {
            var orderGuide = "PK,KG,01,02,03,04,05,06,07,08,09,10,11,12,13,PS,AP,UG,Other".split(",");

            var aIndex = orderGuide.indexOf(a);
            var bIndex = orderGuide.indexOf(b);

            if (aIndex == bIndex) {
                return 0;
            }
            return aIndex > bIndex ? 1 : -1;
        }

        if (gradeLevels === undefined) {
            return "";
        }
        var out = [];

        for (var x = 0; x < gradeLevels.length; x++) {
            out.push(gradeLevels[x]);
        }
        out.sort(sortGradeLevels);
        return out.join(",");
    };

    $scope.getPrimarySubjectLabel = function (primarySubject) {
        if(!primarySubject){
            return "";
        }

        var out = [];
        if(primarySubject.category){
            out.push(primarySubject.category);
        }

        if(primarySubject.subject){
            out.push(primarySubject.subject);
        }

        return out.join(": ");
    };


    /**
     * Build the standards label:
     * @param standards
     * @return label string
     */
    $scope.buildStandardLabel = function (standards) {
        if (standards == null || standards.length == 0) {
            return "";
        }

        var out = standards[0].dotNotation;

        if (standards.length == 1) {
            return out;
        }

        return out + " plus " + (standards.length - 1) + " more";
    };


    $scope.buildStandardTooltip = function (standards) {
        if (!standards) {
            return "";
        }
        var out = [];

        if( standards.length == 1 && standards[0].standard){
            return standards[0].standard;
        }

        for (var i = 0; i < standards.length; i++) {

            if( standards[i] == undefined || standards[i].standard == undefined ){
                return "";
            }
            var wordArray =  standards[i].standard.split(/\W+/);

            var standardLabel = wordArray.length > 6 ? wordArray.splice(0,6).join(" ") + "..." : wordArray.join(" ");
            out.push(standards[i].dotNotation + ": " + standardLabel);
        }

        return out.join(", ");
    };

    $scope.deleteItem = function(item) {
        $scope.itemToDelete = item;
        $scope.showConfirmDestroyModal = true;
    }

    $scope.deleteConfirmed = function(){
        var deletingId = $scope.itemToDelete.id;
        ItemService.remove({id: $scope.itemToDelete.id},
            function(result) {
                $scope.itemToDelete = null;
                $scope.search();
            }
        );
        $scope.itemToDelete = null;
        $scope.showConfirmDestroyModal = false;
    };

    $scope.deleteCancelled = function(){
        console.log("Item Delete Cancelled");
       $scope.itemToDelete = null;
       $scope.showConfirmDestroyModal = false;
    };


    /*
     * called from the repeater. scope (this) is the current item
     */
    $scope.openEditView = function () {
        $location.url('/edit/' + this.item.id);
    };

    $scope.$watch('accessToken.token', function (newValue, oldValue) {
        if (newValue) {
            $timeout(function () {
                $scope.search();
            });
        }
    });
}

HomeController.$inject = ['$scope', '$rootScope','$timeout', '$http', '$location', 'AccessToken', 'ItemService', 'ServiceLookup', 'SupportingMaterial','SearchService'];

