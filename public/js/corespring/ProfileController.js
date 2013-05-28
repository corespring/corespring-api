function ProfileController($scope, Config, Item, ItemFormattingUtils, ResourceUtils) {

  $scope.changeSupportingMaterialPanel = function (sm) {
    $scope.changePanel(sm.name);
    $scope.currentSm = sm;
  };

  $scope.getItemSrc = function (forPrinting) {
    return ResourceUtils.getItemSrc($scope.itemData, forPrinting)
  };

  $scope.getSmSrc = function (sm, forPrinting) {
    return $scope.currentSm ? ResourceUtils.getSmSrc($scope.itemData, sm, forPrinting) : null;
  };

  $scope.getLicenseTypeUrl = ResourceUtils.getLicenseTypeUrl;

  $scope.getCopyrightUrl = ItemFormattingUtils.getCopyrightUrl;


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
            case 307:
              $scope.noRightToView = true;
          }
        }
      }
    );
  }

  loadItemById(Config.itemId);

  if (Config.tab == "")
    $scope.currentPanel = "profile";
  else switch (Config.tab) {
    case 'profile':
      $scope.currentPanel = "profile";
      break;
    case 'item':
      $scope.currentPanel = "item";
      break;
  }
}

ProfileController.$inject = ['$scope', 'Config', 'Item', 'ItemFormattingUtils', 'ResourceUtils'];