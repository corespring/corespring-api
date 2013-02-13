function PreviewController($scope, $timeout, Config, Item, ServiceLookup, ItemFormattingUtils) {

  $scope.itemUrl = "";

  $scope.changeSupportingMaterialPanel = function (sm) {
    $scope.changePanel(sm.name);
    $scope.currentSm = sm;
  };

  $scope.getItemSrc = function (forPrinting) {
    if ($scope.itemData == undefined) return null;
    var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printResource' : 'renderResource');
    var key = $scope.itemData.id;
    return templateUrl.replace("{key}", key);
  };

  $scope.getSmSrc = function (sm, forPrinting) {
    //var templateUrl = ServiceLookup.getUrlFor('previewFile');
    var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printSupportingMaterial' : 'renderResource');
    var key = $scope.itemData.id + "/" + sm.name;
    //empty it so we trigger a refresh
    return templateUrl.replace("{key}", key);
  };

  $scope.getLicenseTypeUrl = function (ltype) {
    return ltype ? "/assets/images/licenseTypes/" + ltype + ".png" : undefined;
  }

  $scope.getCopyrightUrl = function (item) {
    if (!item) return;
    var cname = item.copyrightImageName;
    if (!cname) {
      switch (item.copyrightOwner) {
        case "New York State Education Department":
          cname = "nysed.png";
          break;
        case "State of New Jersey Department of Education":
          cname = "njded.png";
          break;
        case "Illustrative Mathematics":
          cname = "illustrativemathematics.png";
          break;
        case "Aspire Public Schools":
          cname = "aspire.png";
          break;
        case "College Board":
          cname = "CollegeBoard.png";
          break;
        case "New England Common Assessment Program":
          cname = "NECAP.jpg";
          break;
      }
    }
    return cname ? "/assets/images/copyright/" + cname : undefined;
  }

  $scope.printCurrent = function () {

    var features = "width=650,height=800,menubar=yes,location=yes,resizable=yes,scrollbars=yes,status=yes";

    function getPrintUrl(panel) {
      switch (panel) {
        case "profile" :
          return ServiceLookup.getUrlFor('printProfile').replace("{key}", $scope.itemData.id);
        case "item" :
          return $scope.getItemSrc(true);
        default :
          return $scope.getSmSrc($scope.currentSm, true);
      }
    }

    var url = getPrintUrl($scope.currentPanel);

    var newWindow = window.open(url, 'name', features);

    if (newWindow) {
      newWindow.focus();
    }
  };

  $scope.changePanel = function (panelName) {
    $scope.currentSm = null;
    $scope.currentPanel = panelName;
    if (panelName == 'item') {
      $scope.itemUrl = $scope.getItemUrl();
    }
  };

  $scope.getItemUrl = function () {
    if (!$scope.itemData || $scope.currentPanel != 'item') return "";
    return "/web/show-resource/" + $scope.itemData.id + "/data/main";
  };

  $scope.prependHttp = ItemFormattingUtils.prependHttp;

  function loadItemById(id) {
    $scope.noRightToView = false;
    Item.get(
      {
        id: id
      },
      function onItemLoaded(itemData) {
        $scope.itemData = itemData;
        $timeout(function () {
          MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }, 200);
      },
      function onError(error) {
        if (error.data) {
          switch (error.data.code) {
            case 307: $scope.noRightToView = true;
          }
        }
      }
    );
  }

  $scope.$on("requestLoadItem", function (res, id) {
    $scope.currentPanel = "profile";
    $scope.itemData = undefined;
    $scope.itemUrl = "";
    loadItemById(id);
  });

  $scope.loadItem = function () {
    if (Config.itemId == undefined || Config.itemId.length == 0) return;
    loadItemById(Config.itemId);
  };


  $scope.itemId = Config.itemId;
  $scope.loadItem();
  $scope.currentPanel = "profile";
}

PreviewController.$inject = ['$scope', '$timeout', 'Config', 'Item', 'ServiceLookup', 'ItemFormattingUtils'];