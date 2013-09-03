angular.module("lti-chooser",
  ['tagger.services',
    'lti-services',
    'ngResource',
    'cs',
    'corespring-directives',
    'corespring-services',
    'corespring-utils',
    'ui']);

angular.module("lti-chooser").config(['$routeProvider', function ($routeProvider) {
  $routeProvider.when('/main', {templateUrl: '/lti/chooser/partials/main', controller: MainController});
  $routeProvider.when('/browse', {templateUrl: '/lti/chooser/partials/browse', controller: BrowseController});
  $routeProvider.when('/view/:itemId', {templateUrl: '/lti/chooser/partials/view', controller: ltiChooser.ViewItemController});
  $routeProvider.otherwise({redirectTo: '/main'});
}]);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function ($resource) {
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }])

  .factory("LtiItemService", ['$resource', 'ServiceLookup', function ($resource, ServiceLookup) {
    var url = ServiceLookup.getUrlFor('itemDetails');
    return $resource(url, {});
  }]);


function LtiChooserController($scope, $rootScope, $location, LaunchConfigService, Config) {

  $scope.returnToSearch = function () {
    $rootScope.item = null;
    $location.url("/browse");
  };

  $rootScope.hasItemId = function () {
    return $rootScope.getItemId() != null;
  };

  $rootScope.getItemId = function () {
    if ($scope.quiz && $scope.quiz.question && $scope.quiz.question.itemId) {
      return $scope.quiz.question.itemId;
    }
    return null;
  };

  $scope.showSearch = function () {
    return $location.url() == "/browse";
  };

  $scope.showSearchHeader = function () {
    return $location.url().indexOf("/view/") === 0;
  };

  $scope.showPager = function () {
    return ($scope.quiz && !$scope.quiz.question.itemId) && $scope.itemCount;
  };

  $scope.loadItem = function (id) {
    $location.url("/view/" + id);
  };

  $scope.loadMore = function () {
    $rootScope.$broadcast('loadMoreSearchResults');
  };

  $scope.change = function () {
    $location.url("/browse");
  };

  $scope.getAssignRemoveLabel = function () {
    return $scope.isAssigned() ? "Remove" : "Assign";
  };

  $scope.getRemoveTooltip = function () {
    if ($scope.isRemoveDisabled()) {
      return "Can't unassign because students have interacted with this item";
    }
    return "";
  };

  $scope.isRemoveDisabled = function () {
    return $scope.quiz && $scope.quiz.participants && $scope.quiz.participants.length > 0;
  };

  $scope.isAssigned = function () {

    if (!$rootScope.hasItemId()) {
      return false;
    }

    if (!$scope.item) {
      return false;
    }

    return $scope.quiz.question.itemId === $scope.item.id;
  };

  $scope.remove = function () {

    if ($scope.isRemoveDisabled()) {
      return;
    }

    $scope.quiz.question.itemId = null;
  };

  //callback for confirm-popup directive
  $scope.onUnassignConfirmed = function () {
    $rootScope.$broadcast('saveConfig', { redirect: false });
    $location.url("/browse");
  };

  $scope.assign = function () {
    $scope.quiz.question.itemId = $scope.item.id;
    $rootScope.$broadcast('saveConfig');
  };

  $scope.loadSavedSearchParams = function () {
    var params = null;
    if (localStorage) {
      params = localStorage.getItem("item-chooser-search-params")
    }
    if (params) {
      $rootScope.searchParams = JSON.parse(params);
    }
  };

  $scope.watchSearchParams = function () {
    $rootScope.$on("beginSearch", function (event) {
      localStorage.setItem("item-chooser-search-params", JSON.stringify($rootScope.searchParams));
    });
  };

  $scope.init = function () {

    $scope.loadSavedSearchParams();
    $scope.watchSearchParams();

    $scope.quizId = Config.quizId;

    if (!$scope.quizId) {
      throw "No quizId defined - can't load configuration";
    }

    LaunchConfigService.get({id: $scope.quizId}, function (data) {
      $rootScope.quiz = data;

      if (!$rootScope.hasItemId()) {
        $rootScope.$broadcast('search');
      } else {
        $scope.loadItem($scope.getItemId());
      }
    });
  };

  $scope.saveItem = function (onSaveCompleteCallback) {
    LaunchConfigService.save({id: $scope.quiz.id}, $scope.quiz, function (data) {
      $scope.quiz = data;
      if (onSaveCompleteCallback) onSaveCompleteCallback();
    }, function (error) {
      // TODO - implement error handling
      $rootScope.errorMessage = error.data ? error.data : "An error occurred saving your config, please try again";
    });
  };

  $scope.getCollectionTitle = function (c) {
    return c.name.replace("CoreSpring", "");
  };

  $scope.getTitle = function (o) {
    return o.key.replace(/^0/, "")
  };

  $scope.getCollectionSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    var out = _.map(items, function (i) {
      return i.name
    });
    return out.join(", ").replace(/CoreSpring/g, "");
  };

  $scope.getSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    var out = _.map(items, function (i) {
      return i.key
    });
    return out.join(", ").replace(/0/g, "");
  };

  var getMessage = function (items) {
    var msg = "";
    if (items && items.length > 0) {
      msg = items.length + " selected";
    } else {
      msg = "0 selected";
    }
    var b = '<button class="btn btn-mini dropdown-toggle nav-button" data-toggle="dropdown">{msg}<span class="caret"></span></button>';
    return b.replace("{msg}", msg);
  };

  $rootScope.$on('saveConfig', function (event, object) {

    var doRedirect = (object && object.redirect === false) ? false : true;

    var onSaveCompleted = function () {
      if (!Config.returnUrl.match(/\?/)) {
        Config.returnUrl = Config.returnUrl + "?";
      }
      var args = [];
      args.push("embed_type=basic_lti");
      var url = document.location.href.replace(document.location.hash, "");
      var encodedUrl = encodeURIComponent(url + "?canvas_config_id=" + $scope.quiz.id);
      args.push("url=" + encodedUrl);
      location.href = Config.returnUrl + args.join('&');
    };

    if (doRedirect) {
      $scope.saveItem(onSaveCompleted);
    } else {
      $scope.saveItem();
    }
  });

  $scope.init();
}

LtiChooserController.$inject = ['$scope', '$rootScope', '$location', 'LaunchConfigService', 'Config'];


