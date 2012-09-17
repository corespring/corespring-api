function HomeController($scope, $timeout, $http, $location, AccessToken, ItemService) {
    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.accessToken = AccessToken;

    $scope.loadItems = function () {
        ItemService.query({ access_token:$scope.accessToken.token, l:200}, function (data) {
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
            out.push(standards[i].dotNotation + ": " + wordArray.splice(0,6).join(" ") + "...");
        }

        return out.join(", ");
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

HomeController.$inject = ['$scope', '$timeout', '$http', '$location', 'AccessToken', 'ItemService'];

