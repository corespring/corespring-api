//jQuery(function($){
//	var pWrapper = $(".anumbered-lines");
//
//	$(window).bind('load', function(){
//		pWrapper.each(function(){
//            console.log($(this));
//
//			var line = 0;
//
//			$(this).find("p").each(function(){
//				var el = $(this),
//					height,
//					lineHeight,
//					number,
//					html = "",
//					_width = 20, //px
//					_shift = 10; //px
//
//				el.css("padding-left", (_width + _shift) + "px");
//				height = el.height();
//				lineHeight = el.css("line-height");
//				lineHeight = lineHeight === "normal" ? getLineHeightFromNormal( el ) : parseInt( lineHeight );
//				number = Math.floor( height / lineHeight );
//
//                console.log("Line height is "+lineHeight);
//
//				html += '<span class="numbers" style=width:' + _width + 'px;margin-left:-' + (_width + _shift) + 'px;>'
//
//				for (var i = 0; i < number; i++) {
//					html += ++line + '<br>';
//				}
//
//				html += '</span>';
//
//				el.find(".numbers").remove();
//				el.prepend( html );
//			});
//		});
//	});
//
//	function getLineHeightFromNormal( el ){
//		var height = el.height(),
//			fontSize = parseInt( el.css("font-size") ),
//			num = Math.floor(height / fontSize);
//
//		for(; height % (height / num) != 0; --num)
//			;
//		return height / num;
//	}
//});
//

qtiDirectives.directive('p',
    function ($timeout) {

        function checkForHeightChange(elm, scope, container) {
            scope.timeoutPromise = $timeout(function () {
                var newHeight = $(elm).height();
                console.log(scope.lastHeight + " - " + newHeight);
                if (scope.lastHeight == newHeight) {
                    container.performRenumber(container.scope);
                }
                scope.lastHeight = newHeight;
                checkForHeightChange(elm, scope, container);
            }, 50);
        }

        return {
            restrict:'E',
            transclude:true,
            require:'^numberedLines',
            scope:true,
            template:"<span class=\"numbers\" style=\"padding-left: 20px; padding-right: 10px; width: 20px; margin-left:-30px\" ng-bind-html-unsafe=\"numbersHtml\"></span><div  ng-transclude></div>",

            link:function (scope, elm, attrs, container) {
                container.addPara(scope, elm);
                scope.timeoutPromise = checkForHeightChange(elm, scope, container);
                scope.$on('$destroy', function () {
                    scope.timeoutPromise.cancel();
                });
            }
        };
    }
);

qtiDirectives.directive('numberedLines', function () {
    return {
        restrict:'C',
        replace:true,
        scope:true,
        controller:function ($scope, $element, $attrs) {
            $scope.lastLine = 0;
            $scope.paras = [];

            this.performRenumber = function () {
                var line = 0;
                var paras = $scope.paras;
                for (var i = 0; i < paras.length; i++) {
                    var el = $(paras[i].element);
                    var height = el.height();
                    var lineHeight = el.css("line-height");
                    lineHeight = lineHeight === "normal" ? getLineHeightFromNormal(el) : parseInt(lineHeight);

                    var number = Math.floor(height / lineHeight);

                    var s = "";
                    for (var j = 0; j < number; j++) {
                        s += (++line) + "<br />";
                    }
                    paras[i].scope.numbersHtml = s;
                }
            }
            this.addPara = function (paraScope, paraElement) {
                $scope.paras.push({scope:paraScope, element:paraElement});
            }
        }
    }
});