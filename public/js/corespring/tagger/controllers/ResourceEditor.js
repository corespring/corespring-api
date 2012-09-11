//Starting this as a controller but may move it to a directive.

/**
 *
 * TODO: Implement a $resource for files?
 * For Now: just use manual POST/DELETE etc.
 *
 * @param $scope
 * @param $rootScope
 * @param $timeout
 * @param $routeParams
 * @param ItemService
 * @param ServiceLookup
 * @param AccessToken
 * @constructor
 */
function ResourceEditor($scope, $rootScope, $timeout, $routeParams, $http, ServiceLookup) {
    $scope.selectedFileImageUrl = '/assets/images/empty.png';
    $scope.showEditor = false;

    $scope.$on('leaveEditor', function (event) {
        $scope.resourceToEdit = null;
    });

    $scope.$on('enterEditor', function (event, resource, showBackNav, urls ) {
        $scope.resourceToEdit = resource;
        $scope.showBackNav = showBackNav;

        /**
         * An upload url template that contains {filename} - which will be replaced with
         * the local file name.
         * urls contains:
         * createFile - file create url
         * uploadFile - upload file url {filename}
         */
        $scope.urls = urls;
    });

    $scope.$watch('resourceToEdit', function (newValue, oldValue) {

        if (!newValue) {
            return;
        }
        $scope.resource = newValue;

        var defaultFile = _.find($scope.resource.files, function (f) {
            return f["default"] == true
        });
        $scope.showFile(defaultFile);
    });

    $scope.leaveEditor = function () {
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

        if (!f) {
            return;
        }

        $timeout(function () {
            $scope.showEditor = f.content ? true : false;
        });
        $scope.selectedFile = f;

        $scope.selectedFileImageUrl = $scope.updateFileImageUrl(f);
    };

    var imageContentTypes = [ 'image/jpg', 'image/png', 'image/jpeg'];

    $scope.updateFileImageUrl = function (f) {

        if (!f) {
            return;
        }

        if (imageContentTypes.indexOf(f.contentType.toLowerCase()) != -1) {
            var templateUrl = ServiceLookup.getUrlFor('previewFile');
            return templateUrl.replace("{key}", $routeParams.itemId + "/" + $scope.resource.name + "/" + f.name);
        } else {
            return '/assets/images/empty.png';
        }
    };

    $scope.renameFile = function (f) {
        $scope.fileToRename = f;
        $scope.showRenameFileModal = true;
    };

    $scope.canRenameFile = function () {
        return $scope.newFilename ? true : false;
    };

    $scope.confirmRenameFile = function () {

        var fileWithSameName = _.find($scope.resource.files, function (f) {
            return f.name == $scope.newFilename
        });

        if (fileWithSameName != null) {
            $scope.nameAlreadyTaken = true;
            return;
        }

        var oldFilename = $scope.fileToRename.name;
        $scope.fileToRename.name = $scope.newFilename;
        $scope.update($scope.fileToRename, oldFilename);
        $scope.clearRename();
    };

    $scope.cancelRenameFile = function () {
        $scope.clearRename();
    };

    $scope.clearRename = function () {
        $scope.showRenameFileModal = false;
        $scope.fileToRename = null;
        $scope.newFilename = null;
        $scope.nameAlreadyTaken = false;
    };

    $scope.makeDefault = function (f) {

        if (!f) {
            return;
        }
        var currentDefault = _.find($scope.resource.files, function (f) {
            return f['default'] == true;
        });

        if (currentDefault == f) {
            return;
        }
        currentDefault['default'] = false;

        $scope.update(f);
        f['default'] = true;
    };

    /**
     * Update the file on the server
     * @param file
     */
    $scope.update = function(file, filename) {
        if( !filename ){
            filename = file.name;
        }

        $http({
            url:$scope.urls.updateFile.replace("{filename}", filename),
            method:"PUT",
            data:file
        }).success(function (data, status, headers, config) {
                $scope.showFile(file);
            }).error(function (data, status, headers, config) {
                throw "Error updating file";
            });

    };


    $scope.getSelectedFileImageUrl = function () {
        if (!$scope.selectedFile || $scope.selectedFile.storageKey) {
            return "/web/empty.png";
        }
        return "/api/v1/files/{storageKey}"
    };

    $scope.removeFile = function (f) {

        if(!f){
            return;
        }

        $http({
            url:$scope.urls.deleteFile.replace("{filename}", f.name),
            method:"DELETE"
        }).success(function (data, status, headers, config) {
                $scope.resource.files.removeItem(f);
            }).error(function (data, status, headers, config) {
                throw "Error deleting file";
            });
    };


    /**
     * file-uploader callback.
     * @param file - the file selected by the user
     * @return a url where this file will be uploaded
     */
    $scope.calculateUploadUrl = function (file) {
        if (file == null) {
            throw "ItemController:calculateUploadUrl - the file is null"
        }
        return $scope.urls.uploadFile.replace("{filename}", file.name);
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

    var defaultName = "myFile";
    var defaultContent = {
        xml:"<root>hello world</root>",
        css:".hello_world{ font-weight: bold; }",
        js:"alert('hello world');",
        html:"<html><body>hello world</body></html>"
    };

    $scope.createNewVirtualFile = function (name) {

        if (name.indexOf("*.") == 0) {
            var now = new Date();
            var type = name.substr(2, name.length);
            name = defaultName + "_" + now.getHours() + "." + now.getMinutes() + "." + now.getSeconds() + "." + type;
        }

        var fileWithSameName = _.find($scope.resource.files, function (f) {
            return f.name == name
        });

        if (fileWithSameName != null) {
            name = "_" + name;
        }

        var newVirtualFile = {
            name:name,
            contentType:type,
            content:defaultContent[type],
            default:false
        };

        $http({
            url:$scope.urls.createFile,
            method:"POST",
            data:newVirtualFile
        }).success(function (data, status, headers, config) {
                $scope.addFile(newVirtualFile);
                $scope.showFile(newVirtualFile);
            }).error(function (data, status, headers, config) {
                throw "Error saving file";
            });
    };
}

ResourceEditor.$inject = [ '$scope',
    '$rootScope',
    '$timeout',
    '$routeParams',
    '$http',
    'ServiceLookup'];
