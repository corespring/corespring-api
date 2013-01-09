(function () {

  window.com = (window.com || {});
  com.qti = (com.qti || {});
  com.qti.helpers = (com.qti.helpers || {});

  com.qti.helpers.QtiHelper = function () {
    var that = this;
    this.assessmentItemWrapper = [
      '<assessmentitem ',
      'print-mode="false" ',
      '>',
      '${contents}',
      '</assessmentitem>'].join("\n");

    this.wrap = function (content) {
      return this.assessmentItemWrapper.replace("${contents}", content);
    };

    this.prepareBackend = function ($backend) {

      var urls = [
        {
          method: 'GET',
          url: '/api/v1/items/itemId/sessions/itemSessionId',
          response: {"id": "itemSessionId", "itemId": "itemId", "start": 1349970769197, "responses": []}
        }
      ];

      for (var i = 0; i < urls.length; i++) {
        var definition = urls[i];
        $backend.when(definition.method, definition.url).respond(200, definition.response);
      }
    };

    this.compileAndGetScope = function ($rootScope, $compile, node) {
      var element = $compile(that.wrap(node))($rootScope);

      try{
        return { element: element.children(), scope: $rootScope.$$childHead};
      }
      catch(e){
        console.error("!!!");
        return { element: element.children()}
      }
    };


    this.setSessionSettings = function ($rootScope, settings) {

      $rootScope.itemSession = ($rootScope.itemSession || {});

      $rootScope.$apply(function () {
        $rootScope.itemSession.settings = settings;
      });
    };

    this.finishSession = function ($rootScope, value) {

      $rootScope.itemSession = ($rootScope.itemSession || {});

      $rootScope.$apply(function () {
        $rootScope.itemSession.isFinished = value;
      });
    };

    this.setFeedbackEnabled = function ($rootScope, itemSession, show) {
      this.setSessionSettings($rootScope, {showFeedback: show});
    };


    /**
     * Set the correctResponses value on the rootScope
     * @param value
     */
    this.setCorrectResponseOnScope = function (rootScope, key, value) {
      rootScope.itemSession = (rootScope.itemSession || {});
      rootScope.itemSession.sessionData = (rootScope.itemSession.sessionData || {});

      rootScope.$apply(function () {
        var obj = {id: key, value: value};
        rootScope.itemSession.sessionData.correctResponses = [obj];
      });
    };

  };

})();

