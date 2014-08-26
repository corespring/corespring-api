//Starting this as a controller but may move it to a directive.

/**
 *
 * TODO: Implement a $resource for files?
 * For Now: just use manual POST/DELETE etc.
 *
 * @param $scope
 * @param $rootScope
 * @param $element
 * @param $timeout
 * @param $routeParams
 * @param ItemService
 * @param ServiceLookup
 * @param AccessToken
 * @constructor
 */
function ResourceEditor($scope, $rootScope, $element, $timeout, $routeParams, $http, ServiceLookup) {

  $scope.selectedFileImageUrl = '/assets/images/empty.png';
  $scope.showEditor = false;
  $scope.previewVisible = false;

  $scope.$on('leaveEditor', function (event) {
    $scope.resourceToEdit = null;
    $scope.lockedFiles = [];
  });

  $scope.$on('enterEditor', function (event, resource, showBackNav, urls, lockedFiles, latest) {
    $scope.resourceToEdit = resource;
    $scope.showBackNav = showBackNav;
    $scope.lockedFiles = (lockedFiles || []);
    $scope.latest = latest === undefined ? true : latest;

    if (latest) {
      $scope.$emit('setEditable');
    } else {
      $scope.$emit('setUneditable');
    }

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


  $scope.getAceMode = function(contentType) {
    if (!contentType) {
      return "unknown";
    }
    var split = contentType.split("/");
    return split.length == 2 ? split[1] : "unknown";
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
      return templateUrl.replace("{key}", $routeParams.itemId + "/" + $scope.resourceToEdit.name + "/" + f.name);
    } else {
      return '/assets/images/empty.png';
    }
  };

  function isLockedFile(file) {
    if (!$scope.lockedFiles) {
      return false;
    }
    return $scope.lockedFiles.indexOf(file.name) != -1;
  }

  $scope.canShowFileDropdown = function (f) {
    return !isLockedFile(f);
  };

  $scope.canRemove = function (f) {
    return (f && !isLockedFile(f));
  };

  $scope.canRename = function (f) {
    return (f && !isLockedFile(f));
  };

  function isTypeDefaultable(type) {
    if (!type) {
      return false;
    }

    return ["text/html", "application/pdf"].indexOf(type) != -1;
  }

  $scope.canMakeDefault = function (f) {
    if ($scope.lockedFiles && $scope.lockedFiles.length > 0) {
      return false;
    }
    return isTypeDefaultable(f.contentType);
  };

  $scope.onFileSizeGreaterThanMax = function (file, maxSize) {
      function kbToMb(size) {
          return Math.round(size/1024 * 100) / 100
      }
      alert("The file size is too big. CoreSpring limits the size of files that you may upload to " + kbToMb(maxSize) + "MB.");
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
    var fileToUpdate = $scope.fileToRename;
    //replaced update with this to include rollback of file name if rename fails
    $http({
      url: $scope.urls.updateFile.replace("{filename}", oldFilename),
      method: "PUT",
      data: validateFile($scope.fileToRename)
    }).success(function (data, status, headers, config) {
        $scope.showFile($scope.fileToRename);
        $scope.saveSelectedFileFinished();
    }).error(function (data, status, headers, config) {
      if (typeof data.flags != "undefined" && _.contains(data.flags, "alert_increment")) {
        fileToUpdate.name = oldFilename;
        $scope.saveSelectedFileFinished(true);
      } else $scope.saveSelectedFileFinished();
    });

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

  $scope.makeDefault = function(f) {

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
    f['default'] = true;
    $scope.update(f);
  };

  $scope.$on("saveSelectedFile", function() {
    $scope.update($scope.selectedFile);
  });

  $scope.$on("setUneditable", function() {
    $('.not-latest', $element).delay(500).slideDown();
  });

  $scope.$on("setEditable", function() {
    $('.not-latest', $element).slideUp();
  });

  /**
   * Ensure that only text types have the 'content' property'
   */
  function validateFile(f) {

    var textTypes = ['html', 'js', 'css', 'xml', 'txt'];

    function isTextType(){

      for(var i = 0; i < textTypes.length; i++){
        if(f.contentType.indexOf(textTypes[i]) !== -1){
           return true;
        }
      }
      return false;
    }

    if( typeof(f.content) === 'string' && !isTextType()){
      delete f.content;
    }

    return f;
  }

  /**
   * Update the file on the server
   * @param file
   */
  $scope.update = function (file, filename) {

    filename = filename || file.name;

    file = validateFile(file);

    $http({
      url: $scope.urls.updateFile.replace("{filename}", filename),
      method: "PUT",
      data: file
    }).success(function (data, status, headers, config) {
        $scope.showFile(file);
        $scope.saveSelectedFileFinished();
      }).error(function (data, status, headers, config) {
        if (typeof data.flags != "undefined" && _.contains(data.flags, "alert_increment")) {
          $scope.saveSelectedFileFinished(true);
        } else $scope.saveSelectedFileFinished();

      });
  };

  $scope.getSelectedFileImageUrl = function () {
    if (!$scope.selectedFile || $scope.selectedFile.storageKey) {
      return "/web/empty.png";
    }
    return "/api/v1/files/{storageKey}"
  };

  $scope.confirmRemoveFile = function () {
    var f = $scope.fileToRemove;

    if (!f) {
      return;
    }

    $http({
      url: $scope.urls.deleteFile.replace("{filename}", f.name),
      method: "DELETE"
    }).success(function (data, status, headers, config) {
        $scope.resource.files.removeItem(f);
        $scope.showRemoveFileModal = false;
        $scope.fileToRemove = null;
      }).error(function (data, status, headers, config) {
        $scope.showRemoveFileModal = false;
        $scope.fileToRemove = null;
        alert("Error deleting file");
      });
  };

  $scope.cancelRemoveFile = function () {
    $scope.showRemoveFileModal = false;
    $scope.fileToRemove = null;
  };


  $scope.removeFile = function (f) {
    $scope.fileToRemove = f;
    $scope.showRemoveFileModal = true;
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

  $scope.onFileUploadCompleted = function (result,status) {
    if(status == 200){
      var file = JSON.parse(result);
      $scope.addFile(file);
      $scope.showFile(file);
    }else{
      var jsresult = JSON.parse(result);
      if (typeof jsresult.flags != "undefined" && _.contains(jsresult.flags, "alert_increment")) {
        $rootScope.$broadcast('showSaveWarning')
      }
    }
  };


  $scope.addFile = function (file) {
    $scope.resource.files = ($scope.resource.files || []);
    $scope.resource.files.push(file);
  };

  var defaultName = "myFile";
  var defaultContent = {
    xml: "<root>hello world</root>",
    css: ".hello_world{ font-weight: bold; }",
    js: "alert('hello world');",
    html: "<html><head></head><body>hello world</body></html>"
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
      name: name,
      contentType: type,
      content: defaultContent[type],
      "default": false
    };

    $http({
      url: $scope.urls.createFile,
      method: "POST",
      data: newVirtualFile
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
  '$element',
  '$timeout',
  '$routeParams',
  '$http',
  'ServiceLookup'];
