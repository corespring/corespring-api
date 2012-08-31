//Starting this as a controller but may move it to a directive.

function ResourceEditor($scope, $rootScope, $timeout, $routeParams, ItemService, ServiceLookup, AccessToken) {
    $scope.selectedFileImageUrl = '/assets/images/empty.png';
    $scope.showEditor = false;

    $scope.$on('leaveEditor', function(event){
        console.log("[ResourceEditor] on leaveEditor");
        $scope.resourceToEdit = null;
    });

    $scope.$on('enterEditor', function( event, resource, showBackNav ){
        $scope.resourceToEdit = resource;
        $scope.showBackNav = showBackNav;
    });

    $scope.$watch('resourceToEdit', function (newValue, oldValue) {

        if (!newValue) {
            return;
        }
        console.log("resource to edit updated");
        $scope.resource = newValue;

        var defaultFile = _.find($scope.resource.files, function (f) {
            return f["default"] == true
        });
        $scope.showFile(defaultFile);
    });

    $scope.leaveEditor = function(){
      $rootScope.$broadcast('leaveEditor');
    };

    /**
     * Depending on the file type show it.
     * Eg: if its Virtual - show the editor text
     * if its an image - show the image
     * if its another type - launch it in a new window (eg: pdf/doc).
     * @param f
     */
    $scope.showFile = function (f) {
        $timeout(function () {
            $scope.showEditor = f.content ? true : false;
        });
        $scope.selectedFile = f;

        $scope.selectedFileImageUrl = $scope.updateFileImageUrl(f);
        console.log("selectedFile content: " + $scope.selectedFile.content);
    };

    $scope.updateFileImageUrl = function (f) {
        //TODO..
        if (f.storageKey && ['png', 'jpg'].indexOf(f.contentType) != -1) {
            return 'http://funny.ph/wp-content/uploads/tdomf/2334/cute-pug-dog-executive-chair.jpg';
        } else {
            return '/assets/images/empty.png';
        }
    };


    $scope.renameFile = function(f){
        $scope.fileToRename = f;
        $scope.showRenameFileModal = true;
    };

    $scope.canRenameFile = function(){
      return $scope.newFilename ? true : false;
    };

    $scope.confirmRenameFile = function(){
        $scope.fileToRename.name = $scope.newFilename;
        $scope.clearRename();
    };

    $scope.cancelRenameFile = function(){
        $scope.clearRename();
    };

    $scope.clearRename = function(){
        $scope.showRenameFileModal = false;
        $scope.fileToRename = null;
        $scope.newFilename = null;
    };

    $scope.makeDefault = function(f){

        if(!f){
           return;
        }
        var currentDefault = _.find($scope.resource.files, function(f){return f['default'] == true;})

        if( currentDefault == f){
            return;
        }
        currentDefault['default'] = false;
        f['default'] = true;
    };


    $scope.getSelectedFileImageUrl = function () {
        if (!$scope.selectedFile || $scope.selectedFile.storageKey) {
            return "/web/empty.png";
        }
        return "/api/v1/files/{storageKey}"
    };

    $scope.removeFile = function (f) {

        var removedItem = $scope.resource.files.removeItem(f);

        if (!removedItem == f) {
            throw "Couldn't remove file";
        }
    };

    $scope.onFileUploadCompleted = function (result) {
        console.log("onFileUploadCompleted");
        var file = JSON.parse(result);
        $scope.addFile(file);
        $scope.showFile(file);
    };

    $scope.addFile = function (file) {
        $scope.resource.files = ($scope.resource.files || []);
        $scope.resource.files.push(file);
    };

    $scope.createNewVirtualFile = function (name) {

        var newVirtualFile = {
            name:name,
            contentType:"html",
            content:"<html><body>hello</body></html>"
        };

        $scope.addFile(newVirtualFile);
        $scope.showFile(newVirtualFile);
    };
}

ResourceEditor.$inject = [ '$scope',
    '$rootScope',
    '$timeout',
    '$routeParams',
    'ItemService',
    'ServiceLookup',
    'AccessToken'];
