function ProfileController($scope, $timeout, Config, Item, ItemFormattingUtils, MessageBridge) {

  $scope.hidePopup = function () {
    MessageBridge.sendMessage('parent', {message: 'closeProfilePopup'});
  };

  $scope.changeSupportingMaterialPanel = function (sm) {
    $scope.changePanel(sm.name);
    $scope.currentSm = sm;
  };

  $scope.getItemSrc = function (forPrinting) {
    if ($scope.itemData == undefined) return null;
    var templateUrl = forPrinting ? "/web/print-resource/{key}/data/main" : '/web/show-resource/{key}';
    var key = $scope.itemData.id;
    return templateUrl.replace("{key}", key);
  };

  $scope.getSmSrc = function (sm, forPrinting) {
    var templateUrl = forPrinting ? '/web/print-resource/{key}' : "/web/show-resource/{key}";
    var key = $scope.itemData.id + "/" + sm.name;
    return templateUrl.replace("{key}", key);
  };

  $scope.getLicenseTypeUrl = function (ltype) {
    return ltype ? "/assets/images/licenseTypes/" + ltype + ".png" : undefined;
  }

  $scope.getCopyrightUrl = function (item) {
    if (!item) return;
    var cname = ""
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
      case "LearnZillion":
        cname = "lzlogo-png.png";
        break;
    }
    return cname != "" ? ("/assets/images/copyright/" + cname) : undefined;
  }

  $scope.printCurrent = function () {

    var features = "width=650,height=800,menubar=yes,location=yes,resizable=yes,scrollbars=yes,status=yes";

    function getPrintUrl(panel) {
      switch (panel) {
        case "profile" :
          return "/player/item/{key}/profile?tab=profile".replace("{key}", $scope.itemData.id);
        case "item" :
          return "/player/item/{key}/profile?tab=item".replace("{key}", $scope.itemData.id);
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
        itemId: id
      },
      function onItemLoaded(itemData) {
        $scope.itemData = itemData;
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

  loadItemById(Config.itemId);

  if (Config.tab == "")
    $scope.currentPanel = "profile";
  else switch (Config.tab) {
    case 'profile': $scope.currentPanel = "profile"; break;
    case 'item': $scope.currentPanel = "item"; break;
  }
}

ProfileController.$inject = ['$scope', '$timeout', 'Config', 'Item', 'ItemFormattingUtils','MessageBridge'];