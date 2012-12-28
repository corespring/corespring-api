// Generated by CoffeeScript 1.3.3

/*
----------------------------------------------------------
Corespring Angular components (corespring-ng-components)
@link https://github.com/thesib/corespring-ng-components
----------------------------------------------------------
*/


(function() {



}).call(this);
// Generated by CoffeeScript 1.3.3
(function() {

  angular.module('cs.directives', []);

  angular.module('cs', ['cs.directives']).value('cs.config', {});

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
aceEditor directive usage:
<div ace-editor
 ace-model="myText"
 ace-resize-trigger="some"
 ace-theme="sometheme"
 ace-mode="mode"></div>
dependencies:
ace.js + whatever theme and mode you wish to use
@param ace-model - a ng model that contains the text to display in the editor. When the code is changed in
the editor this model will be updated.
@param ace-resize-events - a comma delimited list of ng events that that should trigger a resize
@param ace-theme - an ace theme - loads them using "ace/theme/" + the them you specify. (you need to include the js for it)
@param ace-mode - an ace mode - as above loads a mode.
*/


(function() {

  angular.module('cs.directives').directive('aceEditor', function($timeout) {
    var definition;
    definition = {
      replace: true,
      template: "<div/>",
      link: function(scope, element, attrs) {
        /*
              Apply a nested value..
        */

        var applyValue, attachResizeEvents, initialData, onExceptionsChanged, theme;
        applyValue = function(obj, property, value) {
          var nextProp, props;
          if (!(obj != null)) {
            throw "Cannot apply to null object the property:  " + property + " with value: " + value;
          }
          if (property.indexOf(".") === -1) {
            scope.$apply(function() {
              return obj[property] = value;
            });
          } else {
            props = property.split(".");
            nextProp = props.shift();
            applyValue(obj[nextProp], props.join("."), value);
          }
          return null;
        };
        /* 
        # Attach a listener for events that need to trigger a resize of the editor.
        # @param events
        */

        attachResizeEvents = function(events) {
          var event, eventsArray, _i, _len;
          eventsArray = events.split(",");
          for (_i = 0, _len = eventsArray.length; _i < _len; _i++) {
            event = eventsArray[_i];
            scope.$on(event, function() {
              return $timeout(function() {
                scope.editor.resize();
                if (scope.aceModel != null) {
                  return scope.editor.getSession().setValue(scope.$eval(scope.aceModel));
                }
              });
            });
          }
          return null;
        };
        onExceptionsChanged = function(newValue, oldValue) {
          var exception, _i, _j, _len, _len1;
          if (oldValue != null) {
            for (_i = 0, _len = oldValue.length; _i < _len; _i++) {
              exception = oldValue[_i];
              scope.editor.renderer.removeGutterDecoration(exception.lineNumber - 1, "ace_failed");
            }
          }
          if (newValue != null) {
            for (_j = 0, _len1 = newValue.length; _j < _len1; _j++) {
              exception = newValue[_j];
              scope.editor.renderer.addGutterDecoration(exception.lineNumber - 1, "ace_failed");
            }
          }
          return null;
        };
        if (attrs["aceResizeEvents"] != null) {
          attachResizeEvents(attrs["aceResizeEvents"]);
        }
        if (attrs["aceExceptions"] != null) {
          scope.$watch(attrs["aceExceptions"], onExceptionsChanged);
        }
        scope.editor = ace.edit(element[0]);
        scope.editor.getSession().setUseWrapMode(true);
        theme = attrs["aceTheme"] || "eclipse";
        scope.editor.setTheme("ace/theme/" + theme);
        scope.$watch(attrs["aceMode"], function(newValue, oldValue) {
          var AceMode, modeFactory;
          if (!(newValue != null)) {
            return;
          }
          if (!(scope.editor != null)) {
            return;
          }
          if (newValue === "js") {
            newValue = "javascript";
          }
          modeFactory = require("ace/mode/" + newValue);
          if (!(modeFactory != null)) {
            return;
          }
          AceMode = modeFactory.Mode;
          scope.editor.getSession().setMode(new AceMode());
          return null;
        });
        scope.aceModel = attrs["aceModel"];
        scope.$watch(scope.aceModel, function(newValue, oldValue) {
          if (scope.changeFromEditor) {
            return;
          }
          $timeout(function() {
            scope.suppressChange = true;
            scope.editor.getSession().setValue(newValue);
            scope.suppressChange = false;
            return null;
          });
          return null;
        });
        initialData = scope.$eval(scope.aceModel);
        scope.editor.getSession().setValue(initialData);
        return scope.editor.getSession().on("change", function() {
          var newValue;
          if (scope.suppressChange) {
            return;
          }
          scope.changeFromEditor = true;
          newValue = scope.editor.getSession().getValue();
          applyValue(scope, scope.aceModel, newValue);
          scope.changeFromEditor = false;
          return null;
        });
      }
    };
    return definition;
  });

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
file-uploader - a directive for providing a file upload utility.

Usage:
	<a file-uploader fu-url="/file-upload" fu-name="my-file">add file</a>

  @params
    fu-url (string or name of function on scope)
    fu-name (the item name [only relevant for multipart upload])
    fu-mode (raw|multipart [default: multipart]) 
      raw places the data directly into the content body
      multipart create the 'content flags' eg:  'content-disposition', 'boundary'
    fu-max-size the max size in kB that a user can upload (default: 200)

Events:
  "uploadStarted"  (event) ->  : fired when uploading has started
  "uploadCompleted" (event, serverResponse)-> : fired when uploading is completed

Dependencies: JQuery

TODO: Support file drag and drop
*/


(function() {

  angular.module('cs.directives').directive('fileUploader', function($rootScope) {
    var definition;
    definition = {
      replace: false,
      link: function(scope, element, attrs) {
        var $fuHiddenInput, createFileInput, fuUid, handleFileSelect, maxSize, maxSizeKb, mode, onLocalFileLoadEnd, uploadClick;
        mode = "multipart";
        if (attrs.fuMode === "raw") {
          mode = "raw";
        }
        maxSizeKb = parseInt(attrs.fuMaxSize) || 200;
        maxSize = maxSizeKb * 1024;
        fuUid = "file_upload_input_" + (Math.round(Math.random() * 10000));
        $fuHiddenInput = null;
        uploadClick = function() {
          return $fuHiddenInput.trigger('click');
        };
        createFileInput = function() {
          var styleDef,
            _this = this;
          styleDef = "position: absolute; left: 0px; top: 0px; width: 0px; height: 0px; visibility: hidden; padding: 0px; margin: 0px; line-height: 0px;";
          scope.fileInput = "<input \n       type=\"file\" \n       id=\"" + fuUid + "\"\n       style=\"" + styleDef + "\" \n       name=\"" + attrs.fuName + "\">\n</input>";
          $(element).parent().append(scope.fileInput);
          $fuHiddenInput = $(element).parent().find("#" + fuUid);
          $fuHiddenInput.change(function(event) {
            return handleFileSelect(event);
          });
          return null;
        };
        handleFileSelect = function(event) {
          var file, files, reader,
            _this = this;
          files = event.target.files;
          file = event.target.files[0];
          reader = new FileReader();
          reader.onloadend = function(event) {
            return onLocalFileLoadEnd(file, event);
          };
          reader.readAsBinaryString(file);
          return null;
        };
        /*
              Once the file has been read locally - invoke the Multipart File upload.
        */

        onLocalFileLoadEnd = function(file, event) {
          var callback, name, options, uploader, url,
            _this = this;
          if (file.size > maxSize) {
            if (scope[attrs["fuFileSizeGreaterThanMax"]] != null) {
              scope[attrs["fuFileSizeGreaterThanMax"]](file, maxSizeKb);
            }
            $rootScope.$broadcast("fileSizeGreaterThanMax", file, maxSizeKb);
            return;
          }
          url = attrs.fuUrl;
          if (attrs.fuUrl.indexOf("()") !== -1) {
            callback = attrs.fuUrl.replace("(", "").replace(")", "");
            if (typeof scope[callback] === "function") {
              url = scope[callback](file);
            }
          }
          name = attrs.fuName;
          options = {
            onLoadStart: function() {
              return $rootScope.$broadcast("uploadStarted");
            },
            onUploadComplete: function(responseText) {
              if (scope[attrs["fuUploadCompleted"]] != null) {
                scope[attrs["fuUploadCompleted"]](responseText);
              }
              return $rootScope.$broadcast("uploadCompleted", responseText);
            }
          };
          if (mode === "raw") {
            uploader = new com.ee.RawFileUploader(file, event.target.result, url, name, options);
          } else {
            uploader = new com.ee.MultipartFileUploader(file, event.target.result, url, name, options);
          }
          uploader.beginUpload();
          return null;
        };
        createFileInput();
        element.bind('click', uploadClick);
        return null;
      }
    };
    return definition;
  });

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
Taken from: 
https://github.com/edeustace/inplace-image-changer
*/


(function() {

  if (!XMLHttpRequest.prototype.sendAsBinary) {
    XMLHttpRequest.prototype.sendAsBinary = function(dataStr) {
      var byteValue, ords, ui8a;
      byteValue = function(x) {
        return x.charCodeAt(0) & 0xff;
      };
      ords = Array.prototype.map.call(dataStr, byteValue);
      ui8a = new Uint8Array(ords);
      this.send(ui8a.buffer);
      return null;
    };
  }

  window.com || (window.com = {});

  com.ee || (com.ee = {});

  /*
  Simplifies the xhr upload api
  */


  this.com.ee.XHRWrapper = (function() {

    function XHRWrapper(file, formBody, url, name, options) {
      var now,
        _this = this;
      this.file = file;
      this.formBody = formBody;
      this.url = url;
      this.name = name;
      this.options = options;
      formBody = this.binaryData;
      now = new Date().getTime();
      this.request = new XMLHttpRequest();
      this.request.upload.index = 0;
      this.request.upload.file = this.file;
      this.request.upload.downloadStartTime = now;
      this.request.upload.currentStart = now;
      this.request.upload.currentProgress = 0;
      this.request.upload.startData = 0;
      this.request.open("POST", this.url, true);
      this.request.setRequestHeader("Accept", "application/json");
      if (this.options.onLoadStart != null) {
        this.options.onLoadStart();
      }
      this.request.onload = function() {
        if (_this.options.onUploadComplete != null) {
          return _this.options.onUploadComplete(_this.request.responseText);
        }
      };
    }

    XHRWrapper.prototype.setRequestHeader = function(name, value) {
      this.request.setRequestHeader(name, value);
      return null;
    };

    XHRWrapper.prototype.beginUpload = function() {
      this.request.sendAsBinary(this.formBody);
      return null;
    };

    return XHRWrapper;

  })();

  /*
  Place the binary data directly into the request body.
  */


  this.com.ee.RawFileUploader = (function() {

    function RawFileUploader(file, binaryData, url, name, options) {
      this.file = file;
      this.binaryData = binaryData;
      this.url = url;
      this.name = name;
      this.options = options;
      this.xhr = new com.ee.XHRWrapper(this.file, this.binaryData, this.url, this.name, this.options);
      this.xhr.setRequestHeader("Accept", "application/json");
    }

    RawFileUploader.prototype.beginUpload = function() {
      return this.xhr.beginUpload();
    };

    return RawFileUploader;

  })();

  /*
  Build up a multipart form data request body
  */


  this.com.ee.MultipartFileUploader = (function() {

    function MultipartFileUploader(file, binaryData, url, name, options) {
      var boundary, formBody, uid;
      this.file = file;
      this.binaryData = binaryData;
      this.url = url;
      this.name = name;
      this.options = options;
      uid = Math.floor(Math.random() * 100000);
      boundary = "------multipartformboundary" + uid;
      formBody = this._buildMultipartFormBody(this.file, this.binaryData, boundary);
      this.xhr = new com.ee.XHRWrapper(this.file, formBody, this.url, this.name, this.options);
      this.xhr.setRequestHeader('content-type', "multipart/form-data; boundary=" + boundary);
      this.xhr.setRequestHeader("Accept", "application/json");
    }

    MultipartFileUploader.prototype.beginUpload = function() {
      return this.xhr.beginUpload();
    };

    MultipartFileUploader.prototype._buildMultipartFormBody = function(file, fileBinaryData, boundary) {
      var fileParams, formBuilder, params;
      formBuilder = new com.ee.MultipartFormBuilder(boundary);
      params = this.options.additionalData;
      fileParams = [
        {
          file: file,
          data: fileBinaryData,
          paramName: this.name
        }
      ];
      return formBuilder.buildMultipartFormBody(params, fileParams, boundary);
    };

    return MultipartFileUploader;

  })();

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
Taken from: 
https://github.com/edeustace/inplace-image-changer
*/


(function() {

  window.com || (window.com = {});

  com.ee || (com.ee = {});

  this.com.ee.MultipartFormBuilder = (function() {

    function MultipartFormBuilder(boundary) {
      this.boundary = boundary;
      this.dashdash = "--";
      this.crlf = "\r\n";
    }

    /*
        fileParams = [
           
            {file : file (File)
            data : fileBinaryData
            paramName : name of request parameter}
          
            ...
            ]
    */


    MultipartFormBuilder.prototype.buildMultipartFormBody = function(params, fileParams) {
      var output,
        _this = this;
      output = "";
      if (params != null) {
        $.each(params, function(i, val) {
          if ((typeof val) === 'function') {
            val = val();
          }
          return output += _this.buildFormSegment(i, val);
        });
      }
      if (fileParams != null) {
        $.each(fileParams, function(i, val) {
          output += _this.buildFileFormSegment(val.paramName, val.file, val.data);
          return null;
        });
      }
      output += this.dashdash;
      output += this.boundary;
      output += this.dashdash;
      output += this.crlf;
      return output;
    };

    MultipartFormBuilder.prototype.buildFormSegment = function(key, value) {
      var contentDisposition;
      contentDisposition = this._buildContentDisposition(key);
      return this._buildFormSegment(contentDisposition, value);
    };

    MultipartFormBuilder.prototype._buildContentDisposition = function(name) {
      var template;
      template = "Content-Disposition: form-data; name=\"${name}\" ";
      return template.replace("${name}", name);
    };

    MultipartFormBuilder.prototype._buildFileContentDisposition = function(formName, fileName) {
      var out;
      this.template = "Content-Disposition: form-data; name=\"${formName}\"; filename=\"${fileName}\" ";
      out = this.template.replace("${formName}", formName);
      out = out.replace("${fileName}", fileName);
      return out;
    };

    MultipartFormBuilder.prototype.buildFileFormSegment = function(formName, file, binaryData) {
      var contentDisposition;
      contentDisposition = this._buildFileContentDisposition(formName, file.name);
      contentDisposition += this.crlf;
      contentDisposition += "Content-Type: " + file.type;
      return this._buildFormSegment(contentDisposition, binaryData);
    };

    MultipartFormBuilder.prototype._buildFormSegment = function(contentDisposition, value) {
      var output;
      output = '';
      output += this.dashdash;
      output += this.boundary;
      output += this.crlf;
      output += contentDisposition;
      output += this.crlf;
      output += this.crlf;
      output += value;
      output += this.crlf;
      return output;
    };

    return MultipartFormBuilder;

  })();

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
 * loading-button
 * Simple hookup to twitter button js. 
 * <btn loading-button ngModel="isLoading" data-loading-text="loading..."></btn>
 * dependencies: jQuery and bootstrap.button js
*/


(function() {

  angular.module('cs.directives').directive('loadingButton', [
    function() {
      var definition;
      definition = {
        require: 'ngModel',
        link: function(scope, element, attrs) {
          return scope.$watch(attrs["ngModel"], function(newValue) {
            var command;
            command = newValue ? "loading" : "reset";
            return $(element).button(command);
          });
        }
      };
      return definition;
    }
  ]);

}).call(this);
// Generated by CoffeeScript 1.3.3
(function() {

  angular.module('cs.directives').directive('multiSelect', function($timeout) {
    var definition;
    definition = {
      replace: true,
      restrict: 'A',
      scope: 'isolate',
      template: "<span class=\"multi-select\">\n  <div \n    class=\"items\" \n    ng-click=\"showChooser=!showChooser\"\n    ng-bind-html-unsafe=\"multiGetSelectedTitle(selected)\">\n  </div>\n  <div class=\"chooser\" ng-show=\"showChooser\">\n   <ul>\n     <li ng-repeat=\"o in options\" >\n       <input type=\"checkbox\" ng-click=\"toggleItem(o)\"></input>\n       {{multiGetTitle(o)}}\n     </li>\n   </ul>\n  </div>\n</span>",
      link: function(scope, element, attrs) {
        var applyValue, changeCallback, getSelectedTitleProp, getTitleProp, modelProp, optionsProp;
        optionsProp = attrs['multiOptions'];
        modelProp = attrs['multiModel'];
        getTitleProp = attrs['multiGetTitle'];
        getSelectedTitleProp = attrs['multiGetSelectedTitle'];
        changeCallback = attrs['multiChange'];
        scope.noneSelected = "None selected";
        scope.showChooser = false;
        scope.$watch(optionsProp, function(newValue) {
          scope.options = newValue;
          return null;
        });
        scope.$watch(modelProp, function(newValue) {
          scope.selected = newValue;
          return null;
        });
        /*
              Apply a nested value..
        */

        applyValue = function(obj, property, value) {
          var nextProp, props;
          if (!(obj != null)) {
            throw "Cannot apply to null object the property:  " + property + " with value: " + value;
          }
          if (property.indexOf(".") === -1) {
            obj[property] = value;
          } else {
            props = property.split(".");
            nextProp = props.shift();
            applyValue(obj[nextProp], props.join("."), value);
          }
          return null;
        };
        /*
              Need to use $eval to support nested values
        */

        scope.toggleItem = function(i) {
          var arr, index, optionIndex, sortFn;
          if (!(scope.$eval(modelProp) != null)) {
            applyValue(scope, modelProp, []);
          }
          arr = scope.$eval(modelProp);
          index = arr.indexOf(i);
          optionIndex = scope.$eval(optionsProp).indexOf(i);
          if (index === -1) {
            arr.push(i);
          } else {
            arr.splice(index, 1);
          }
          sortFn = function(a, b) {
            var aIndex, bIndex;
            aIndex = scope.$eval(optionsProp).indexOf(a);
            bIndex = scope.$eval(optionsProp).indexOf(b);
            return aIndex - bIndex;
          };
          applyValue(scope, modelProp, arr.sort(sortFn));
          if (changeCallback != null) {
            scope[changeCallback]();
          }
          return null;
        };
        scope.multiGetSelectedTitle = function(items) {
          return scope[getSelectedTitleProp](items);
        };
        scope.multiGetTitle = function(t) {
          return scope[getTitleProp](t);
        };
        return null;
      }
    };
    return definition;
  });

}).call(this);
// Generated by CoffeeScript 1.3.3
(function() {

  if (!jQuery.expr[':'].matches_ci) {
    jQuery.expr[':'].matches_ci = function(a, i, m) {
      return jQuery(a).text().toUpperCase() === m[3].toUpperCase();
    };
  }

  angular.module('cs.directives').directive('tagList', function($timeout) {
    var definition;
    definition = {
      replace: false,
      template: "<span/>",
      link: function(scope, element, attrs) {
        var applySelectedTags, availableTags, buildTagList, linkClass, onTagClick, selectedTags;
        selectedTags = null;
        availableTags = null;
        linkClass = attrs.tagListLinkClass || "tag-list-link";
        buildTagList = function() {
          var $link, x, _i, _len,
            _this = this;
          for (_i = 0, _len = availableTags.length; _i < _len; _i++) {
            x = availableTags[_i];
            $link = $("<a class='" + linkClass + "' href='javascript:void(0)'>" + x + "</a>");
            $(element).append($link);
            $link.click(function(event) {
              return onTagClick(event);
            });
          }
          return null;
        };
        applySelectedTags = function() {
          var $link, selectedTag, _i, _len;
          $(element).find("." + linkClass).removeClass('selected');
          for (_i = 0, _len = selectedTags.length; _i < _len; _i++) {
            selectedTag = selectedTags[_i];
            $link = $(element).find("a:matches_ci('" + selectedTag + "')");
            $link.addClass("selected");
          }
          return null;
        };
        onTagClick = function(event) {
          var isCurrentlySelected, tagName;
          tagName = $(event.target).text();
          isCurrentlySelected = $(event.target).hasClass("selected");
          scope.$apply(function() {
            var index, scopeTagArray;
            scopeTagArray = scope.$eval(attrs.selectedTags);
            if (isCurrentlySelected) {
              index = scopeTagArray.indexOf(tagName);
              return scopeTagArray.splice(index, 1);
            } else {
              if (scopeTagArray.indexOf(tagName) === -1) {
                return scopeTagArray.push(tagName);
              }
            }
          });
          applySelectedTags();
          return null;
        };
        scope.$watch(attrs.selectedTags, function(newValue) {
          selectedTags = newValue;
          if ((availableTags != null) && (selectedTags != null)) {
            applySelectedTags();
          }
          return null;
        });
        scope.$watch(attrs.tags, function(newValue) {
          availableTags = newValue;
          if (availableTags != null) {
            buildTagList();
          }
          if (selectedTags != null) {
            applySelectedTags();
          }
          return null;
        });
        return null;
      }
    };
    return definition;
  });

}).call(this);
// Generated by CoffeeScript 1.3.3

/*
infinite scrolling module
Triggers a callback to the scope.
Usage: 
<div when-scrolled="doSomething()"/>
*/


/*
infinite scrolling module
Triggers a callback to the $scope.
Usage: 
<div when-scrolled="doSomething()" scrolled-mode="div|window(default)"/>
*/


(function() {

  angular.module('cs.directives').directive('whenScrolled', function() {
    var linkFn;
    linkFn = function($scope, $elm, $attrs) {
      var mode, onDivScroll, onWindowScroll, raw;
      raw = $elm[0];
      onDivScroll = function() {
        var rectObject, scrollTop, scrolledTotal;
        rectObject = raw.getBoundingClientRect();
        if (rectObject.height === 0) {
          return;
        }
        scrollTop = $elm.scrollTop();
        scrolledTotal = $elm.scrollTop() + $elm.innerHeight();
        if (scrolledTotal >= raw.scrollHeight) {
          $scope.$apply($attrs.whenScrolled);
        }
        return null;
      };
      onWindowScroll = function() {
        var rectObject;
        rectObject = raw.getBoundingClientRect();
        if (rectObject.height === 0) {
          return;
        }
        if (rectObject.bottom <= window.innerHeight) {
          $scope.$apply($attrs.whenScrolled);
        }
        return null;
      };
      mode = $attrs['scrolledMode'] || 'window';
      if (mode === "window") {
        angular.element(window).bind('scroll', onWindowScroll);
      } else {
        $elm.bind('scroll', onDivScroll);
      }
      return null;
    };
    return linkFn;
  });

}).call(this);
