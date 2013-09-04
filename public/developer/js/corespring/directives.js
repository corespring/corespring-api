//directives.js
angular.module('cs.directives', [])

angular.module('cs', ['cs.directives']).value('cs.config', {})

/*
 * Lifted from Pumbaa80
 * TODO - should really create a directive that renders json pretty printed and highlighted
 */

window.com = (window.com || {});
com.cs = (com.cs || {});
com.cs.utils = ( com.cs.utils || {})

com.cs.utils.syntaxHighlightJson = function(data) {
    var json = JSON.stringify(data, null, '    ');
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
};


com.cs.utils.xmlToString = function(xmlData) {
  var xmlString;
  //IE
  if (window.ActiveXObject){
      xmlString = xmlData.xml;
  }
  // code for Mozilla, Firefox, Opera, etc.
  else{
      xmlString = (new XMLSerializer()).serializeToString(xmlData);
  }
  return xmlString;
}


angular.module('cs.directives').directive('jsonHighlight', function($http) {

  var definition = {
    replace: false,
    restrict: 'E',
    transclude: false,
    scope: true,
    link: function( scope, element, attrs){
      var html = $(element).html();
      try{
        var object = JSON.parse(html);
      }
      catch (e){
          // TODO - implement error handling
      }
      var jsonFormatted = com.cs.utils.syntaxHighlightJson(object);
      $(element).html("<div class='responseHolder'><pre>" + jsonFormatted + "</pre></div>");
    }
  };
  return definition;

});

/**
 * A widget for executing rest requests. It shows the REST method and path and adds a button
 * with which the user can see the data that is going to be sent to the server, make the request
 * and see the response.
 *
 * Options:
 * @param resultType - json|xml|file (default: json)
 * @param disabled - locks the ui if true and adds a 'coming soon' label
 * @param method - GET|POST|PUT|DELETE - the http method to use
 * @param path - the path to show to the user (for display only)
 * @param url - the url to use to make the call to the server
 * @requestBody - the data to send as part of the requestBody - adds a text area so the user can edit it.
 * @requestHeaders - a & delimited list of request headers to add to the call - shown to the user in a text input
 * @formParams - a & delimited list of form parameters for the request - shown to the user as text inputs.
 */
angular.module('cs.directives').directive('restWidget', function($http,$rootScope) {

  var definition = {
    replace: true,
    restrict:'E',
    transclude: true,
    scope: true,
    templateUrl: "template.html",

    /*
     * We need the compile phase as we apply a different bind depending on the expected resultType.
     */
    compile: function compile(tElement, tAttrs, transclude) {

      var resultType = (tAttrs["resultType"] || "json" );
      var bindType =  resultType == "json" ? "ng-bind-html-unsafe" : "ng-bind";
      $(tElement).find("#responseBox").attr(bindType, "responseBody");

      /**
       * The standard link function
       */
      var link = function(scope, element, attrs) {

        var $inputField = $(element).find("#urlInput");
        var $requestBody = $(element).find("#requestBody");

        scope.disabled = attrs["disabled"] === "true";
        scope.disabledMessage = ( attrs["disabledMessage"] || "coming soon" );

        if( scope.disabled ){
          scope.widgetCssClass = ( attrs["disabledClass"] || "disabled" );
        }

        scope.method = ( attrs["method"] || "GET");
        scope.path = ( attrs["path"] || "" )
        scope.resultType = ( attrs["resultType"] || "json" )

        scope.showResponseBox = scope.resultType != "file";

        scope.requestBody = ( attrs["requestBody"] || "")
        scope.requestHeaders = (attrs["requestHeaders"] || "")
        var insertAccessTokenHeader = function(){
            if($rootScope.access_token){
                var atindex = scope.requestHeaders.indexOf("Authorization: Bearer ")
                if(atindex != -1){
                    var endindex = scope.requestHeaders.indexOf("&",atindex)
                    if(endindex == -1) {
                        var oldat = scope.requestHeaders.substring(atindex+22)
                        scope.requestHeaders = scope.requestHeaders.replace(oldat,$rootScope.access_token)
                    }else{
                        var oldat = scope.requestHeaders.substring(atindex+22,endindex)
                        scope.requestHeaders = scope.requestHeaders.replace(oldat,$rootScope.access_token)
                    }
                }
            }
            return url
        }
        insertAccessTokenHeader();
        scope.$on('insertAccessTokenHeader', function(){
          insertAccessTokenHeader();
        });
        function showPostField( property, allowedMethods ){
          if( !property || property == "" ){
              return false;
          }
          if( !allowedMethods ){
            return true;
          }
          return allowedMethods.indexOf(scope.method) != -1;
        }

        scope.showRequestBody = showPostField( scope.requestBody, ["POST", "PUT"]);
        scope.showRequestHeaders = showPostField( scope.requestHeaders );


        function buildFormParams( formParamString ){

          if( !formParamString || formParamString == ""){
            return [];
          }

          var splitted = formParamString.split("&")

          var out = _.map( splitted, function(item){

            if( !item || item == "") {
              return { name: "?", value: "?"}
            }

            var splitEquals = item.split("=");
            return { name: splitEquals[0], value: splitEquals[1] };
          } );

          return out;
        }

        scope.formParams =  buildFormParams( (attrs["formParams"] || "") );
          //test if the form param contain client id and secret. if so, then we know this widget is the access token widget
          var hasClientIdAndSecret = function(){
              var hasClientSecret = typeof _.find(scope.formParams,function(formParam){
                  return formParam.name === "client_secret"
              }) !== "undefined"
              var hasClientId = typeof _.find(scope.formParams,function(formParam){
                  return formParam.name === "client_id"
              }) !== "undefined"
              return hasClientSecret && hasClientId
          }
          if(hasClientIdAndSecret()){
              //set the broadcast receiver to set the form params once the api client has been received
              scope.$on('setApiClient',function(){
                  if($rootScope.apiClient){
                      scope.formParams = [
                          {name: "client_id", value: $rootScope.apiClient.clientId},
                          {name: "client_secret", value: $rootScope.apiClient.clientSecret}
                      ]
                  }
              })
          }
        scope.onSuccess = function( result ){

          scope.loading = false;
          if(result.access_token){
              $rootScope.access_token = result.access_token
              $rootScope.$broadcast('insertAccessToken')
              $rootScope.$broadcast('insertAccessTokenHeader')
          }
          var body = ( scope.resultType == "xml" ) ? com.cs.utils.xmlToString(result) : com.cs.utils.syntaxHighlightJson(result);
          scope.$apply( function() {
            scope.responseBody = body;
          })
        };

        scope.onError = function(jqXHR, textStatus, errorThrown){
          //console.error("Error!! " + textStatus);
          //console.error("Error!! " + errorThrown);

          scope.loading = false;
          function validJson(str){
            try{
              JSON.parse(jqXHR.responseText);
              return true;
            }
            catch(e){
              return false;
            }
          };

          var error = validJson(jqXHR.responseText) ? jqXHR.responseText : errorThrown.toString();
          $(element).find('#responseBox').html( error );
        };

        scope.addDotsIntervalId = -1;

        scope.addDots = function(){

          if(scope.loading){

            scope.$apply( function(){
              scope.responseBody += ".";
            });

            scope.addDotsIntervalId = setTimeout( scope.addDots, 1000 );
          } else{
            clearInterval(scope.addDotsIntervalId);
          }
        }

        scope.executeCall = function(){
          var url = $inputField.val();


          $.support.cors = true;

          scope.loading = true;

          scope.$apply( function(){
            scope.responseBody = "Loading...";
          });

          scope.addDots();

          var request = {
            url: url,
            type: scope.method,
            crossDomain: true,
            //cache:false,
            success: function( result ) { scope.onSuccess.call(scope, result ) },
            error: function(jqXHR, textStatus, errorThrown){ scope.onError.call(scope, jqXHR, textStatus, errorThrown)},
            dataType: scope.resultType,
            beforeSend: function(request){
              // special header to let the server know to ignore user session
              request.setRequestHeader("CoreSpring-IgnoreSession", "true");

              if( scope.requestHeaders ){

                var headers = scope.requestHeaders.split("&");

                _.each(headers, function(h){

                  if( !h && h.indexOf(":") == -1){
                    return;
                  }

                  var splitHeader = h.split(":");
                  request.setRequestHeader( splitHeader[0], splitHeader[1] );
                });
              }
            }
          };

          if( scope.method == "POST" || scope.method == "PUT" ){

            if( scope.requestBody != "" ){
              request.data = $requestBody.val();
              request.contentType = "application/json; charset=utf-8";
            }

            if(scope.formParams && scope.formParams.length > 0){

              request.data = {};
              for(var i = 0; i < scope.formParams.length; i++){
                var item = scope.formParams[i];
                request.data[item.name] = item.value;
              }

            }
          }

          $.ajax(request);
        };

          var url = attrs["url"];
          var insertAccessToken = function(){
              if($rootScope.access_token){
                  var atindex = url.indexOf("access_token=")
                  if(atindex != -1){
                      var endindex = url.indexOf("&",atindex)
                      if(endindex == -1) {
                          var oldat = url.substring(atindex+13)
                          return url.replace(oldat,$rootScope.access_token)
                      }else{
                          var oldat = url.substring(atindex+13,endindex)
                          return url.replace(oldat,$rootScope.access_token)
                      }
                  }
              }
              return url
          }
          $(element).find("#urlInput").val(insertAccessToken());
          scope.$on('insertAccessToken', function(){
              $(element).find('#urlInput').val(insertAccessToken())
          });

        var responseDiv = "<div style='width:100%; height: 200px;'></div>";
        scope.showRunner = false;

        scope.$watch("showRunner", function( newValue, oldValue ){

          if( newValue === true){
            $(element).find("#runner").show();
            $(element).find("#tryLabel").html("hide");
            scope.iconClass = "icon-minus";
          }
          else {
            $(element).find("#runner").hide();
            $(element).find("#tryLabel").html("try it");

            scope.iconClass = "icon-plus";
          }
        })

        $(element).find('#tryBtn').click( function(){
            scope.$apply( function() {
              scope.showRunner = !scope.showRunner;
            });
        });

        if( scope.resultType == "file" ){
          $(element).find("#goBtn")
            .attr("href", url )
            .attr("target", "_blank");

        } else {
          $(element).find('#goBtn').click( function(){
              scope.executeCall();
          });

        }
    }
    return link;
    },
  }
  return definition;

}); 
