function HomeController($scope, $timeout, $http, $location, AccessToken, ItemService, ServiceLookup, SupportingMaterial) {
    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.accessToken = AccessToken;

    $scope.loadItems = function () {
        ItemService.query({
            access_token:$scope.accessToken.token,
            l:200,
            f: JSON.stringify( {
                "title":1,
                "primarySubject":1,
                "gradeLevel":1,
                "itemType":1,
                "standards":1} )
            },
            function (data) {
            $scope.items = data;

            //trigger math ml rendering
            $timeout(function(){
                MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
            }, 200);
        });
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

    var deleteDataFile = function(itemId, fileName) {
        console.log("Deleting Data File id: "+itemId+" name: "+fileName);
        var substitutions = { itemId: itemId };
        var deleteFileUrl = ServiceLookup.getUrlFor('deleteDataFile', substitutions);
        var tokenize = function(url){
            return url + "?access_token=" + AccessToken.token;
        }
        $http({
            url:tokenize(deleteFileUrl.replace("{filename}", fileName)),
            method:"DELETE"
        }).success(function (data, status, headers, config) {
                console.log("Delete file success");
        }).error(function (data, status, headers, config) {
                throw "Error deleting file";
        });

    }

    var deleteSupportingMaterial = function(itemId, resourceName) {
        console.log("Deleting Supporint Material: "+itemId+" name: "+fileName);
        SupportingMaterial.delete({access_token:AccessToken.token, itemId:itemId, resourceName: resourceName},
            function (file) {
                console.log("File Getting Succ");
                console.log(file);
            },
            function () {
                console.log("File Getting error");
            }
        );

    }

    $scope.deleteItem = function(item) {
        $scope.itemToDelete = item;
        $scope.showConfirmDestroyModal = true;
    }

    $scope.deleteConfirmed = function(){
        console.log("Item Delete Confirmed");
        console.log($scope.itemToDelete);
        var deletingId = $scope.itemToDelete.id;
        ItemService.get({access_token:$scope.accessToken.token, id: $scope.itemToDelete.id},
            function success(data) {
                //TODO: should we not be doing this on the server side?

                // Delete associated data files
                if (data.data && data.data.files)
                for (var i=0; i < data.data.files.length; i++) {
                    var file = data.data.files[i];
                    if (angular.isUndefined(file.content) || file.content.length < 1)
                        deleteDataFile(deletingId, file.name);
                }

                // Delete associated supporting materials
                if (data.supportingMaterials)
                for (var i=0; i < data.supportingMaterials.length; i++) {
                    var material = data.supportingMaterials[i];
                    for (var j=0; j < material.files.length; j++) {
                        var file = material.files[j];
                        deleteSupportingMaterial(deletingId, material.name);
                    }
                }
            }
        );

        ItemService.remove({access_token:$scope.accessToken.token, id: $scope.itemToDelete.id},
            function(result) {
                console.log("Item Successfully deleted");
                $scope.itemToDelete = null;
                $scope.loadItems();
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
        $location.url('/view/' + this.item.id);
    };

    $scope.$watch('accessToken.token', function (newValue, oldValue) {
        if (newValue) {
            $timeout(function () {
                $scope.loadItems();
            });
        }
    });
}

HomeController.$inject = ['$scope', '$timeout', '$http', '$location', 'AccessToken', 'ItemService', 'ServiceLookup', 'SupportingMaterial'];

