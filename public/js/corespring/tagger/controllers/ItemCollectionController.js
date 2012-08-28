// Controller for the ItemCollectionController view
function ItemCollectionController($scope, ItemData, $location, searchService, $rootScope, Collection) {

    $scope.$root.mode = "itemList";

    // init searchparams model
    $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemData.createWorkflowObject();

    // init collections model
    $scope.collections = Collection.query();

    // broadcast an event when the Edit view is called
    $rootScope.$broadcast('onListViewOpened');

    $scope.itemDataCollection = [];
    /*
     *   ######## METHODS ##########
     */

    // search update function
    $scope.search = function () {
        searchService.search($scope.searchParams, function (data) {
            $scope.itemDataCollection = data;
        });

    };

    // method for loading more items for infinite scroll
    $scope.loadMore = function () {

        searchService.loadMore(function () {
                // re-bind the scope collection to the services model after result comes back
                $scope.itemDataCollection = searchService.itemDataCollection;
            }
        );
    };

    /*
     * called from the repeater. scope (this) is the current item
     */
    $scope.openEditView = function () {
        searchService.currentItem = this.item;

        if( $scope.searchParams ){
            $rootScope.searchParams = $scope.searchParams;
        }

        $location.url('/edit/' + this.item._id.$oid + '?panel=metadata&preview=0&fileList=0');
    };

    $scope.showGradeLevel = function () {
        return $scope.createGradeLevelString(this.item.gradeLevel);
    };

    $scope.showItemType = function(item){

       if( item.itemType != "Other"){
           return item.itemType;
       }
       return item.itemType + ": " + item.itemTypeOther;
    };

    $scope.createGradeLevelString = function (gradeLevels) {

        function sortGradeLevels(a, b){
            var orderGuide = "PK,KG,01,02,03,04,05,06,07,08,09,10,11,12,13,PS,AP,UG,Other".split(",");

            var aIndex = orderGuide.indexOf(a);
            var bIndex = orderGuide.indexOf(b);

            if( aIndex == bIndex ) {
                return 0;
            }
            return aIndex > bIndex ? 1 : -1;
        }


        if(gradeLevels === undefined){
            return "";
        }
        var out = [];

        if( gradeLevels.indexOf == undefined ){
            /**
             * @deprecated - need to remove this model type.
             */
            for (var x  in gradeLevels) {
                if (gradeLevels[x]) {
                    out.push(x);
                }
            }
        } else {
            for( var x = 0 ; x < gradeLevels.length; x++ ){
                out.push(gradeLevels[x]);
            }
        }
        out.sort(sortGradeLevels);
        return out.join(",");
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
            out.push(standards[i].dotNotation + ": " + wordArray.splice(0,6).join(" ") + "...");
        }

        return out.join(", ");
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
    $scope.destroyConfirmed = function(){

        $scope.itemToDelete.destroy( function(result){
            if(result.success){
                $scope.itemToDelete = null;
                $scope.search();
            }
        });

        $scope.showConfirmDestroyModal = false;
    };

    $scope.destroyCancelled = function(){
       $scope.itemToDelete = null;
       $scope.showConfirmDestroyModal = false;
    };

    $scope.deleteItem = function(item){
       $scope.itemToDelete = item;
       $scope.showConfirmDestroyModal = true;

    };

    // initialize with a search
    $scope.search();
}

ItemCollectionController.$inject = ['$scope', 'ItemData', '$location', 'searchService', '$rootScope', 'Collection'];

