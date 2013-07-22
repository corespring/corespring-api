angular.module("qti.directives").directive("line", function(){
    return {
        restrict: "E",
        require: '^lineinteraction',
        scope: 'true',
        compile: function(element,attrs,transclude){
            element.attr('hidden','')
            var locked = element.parent()[0].attributes.getNamedItem('locked')?true:false
            return function(scope,element,attrs,LineCtrl){
                var points = _.map(element.find('point'),function(p){
                    var coords = p.innerText.split(",")
                    if(coords.length == 2){
                        return {x: coords[0], y: coords[1]}
                    }else {
                        throw "each point must contain x and y coordinate separated by a comma"
                    }
                })
                if(points.length == 2){
                    LineCtrl.setInitialParams({
                          points: {
                              A: points[0],
                              B: points[1]
                          },
                          drawShape:{
                              line: ["A","B"]
                          },
                          submission:{lockGraph: locked}
                      })
                } else{
                    throw "line must contain 2 points"
                }
            }
        }
    }
})
angular.module("qti.directives").directive("lineinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div class='points-display'>",
        "       <div class='point-display'>",
        "           <p>Point A:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.A.x' ng-disabled='outcomeReturned'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.A.y'  ng-disabled='outcomeReturned'>",
        "       </div>",
        "       <div class='point-display'>",
        "           <p>Point B:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.B.x' ng-disabled='outcomeReturned'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.B.y' ng-disabled='outcomeReturned'>",
        "       </div>",
        "   </div>",
        "   <div jsx-graph graph-callback='graphCallback' interaction-callback='interactionCallback' max-points='2'></div>",
        "   <div id='initialParams' ng-transclude></div>",
        "</div>"].join("\n"),
        transclude: true,
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', function($scope){
            this.setInitialParams = function(initialParams){
                $scope.initialParams = initialParams
            }
            $scope.$watch('graphCallback',function(){
                if($scope.graphCallback && $scope.initialParams){
                    $scope.graphCallback($scope.initialParams)
                }
            })
            $scope.points = {A: {x: undefined, y: undefined}, B: {x: undefined, y: undefined}}
            $scope.$watch('showNoResponseFeedback', function(){
                 if($scope.isEmptyItem($scope.graphCoords) && $scope.showNoResponseFeedback){
                    $scope.graphCallback({submission: {isIncomplete:true}})
                 }
            });
            $scope.interactionCallback = function(params){
                if(params.points){
                  if(params.points.A){
                    $scope.points.A = params.points.A
                  }
                  if(params.points.B){
                    $scope.points.B = params.points.B
                  }

                  if(params.points.A && params.points.B){
                      $scope.graphCoords = [params.points.A.x+","+params.points.A.y, params.points.B.x+","+params.points.B.y]
                      var slope = (params.points.A.y - params.points.B.y) / (params.points.A.x - params.points.B.x)
                      var yintercept = params.points.A.y - (params.points.A.x * slope)
                      $scope.equation = "y="+slope+"x+"+yintercept
                      $scope.graphCallback({submission:{clearBorder: true},drawShape:{line: ["A","B"]}})
                  }else $scope.graphCoords = null
                  if(!$scope.locked)$scope.controller.setResponse($scope.responseIdentifier, $scope.graphCoords);
                }
            }
            $scope.$watch('points', function(points){
              function checkCoords(coords){
                  return coords && !isNaN(coords.x) && !isNaN(coords.y)
              }
              var graphPoints = {}
              _.each(points,function(coords,ptName){
                if(checkCoords(coords)) graphPoints[ptName] = coords
              })
              if($scope.graphCallback) $scope.graphCallback({points: graphPoints})
            }, true)
            $scope.$on("formSubmitted",function(){
                if(!$scope.locked){
                   $scope.outcomeReturned = true
                   var response = _.find($scope.itemSession.responses,function(r){
                       return r.id === $scope.responseIdentifier
                   })
                   $scope.graphCallback({submission: {isCorrect: response && response.outcome.isCorrect, lockGraph: true}});
                }
            });

            //refresh periodically
            setInterval(function(){
                $scope.$digest()
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            var width = attrs.graphWidth?attrs.graphWidth:"300px"
            var height = attrs.graphHeight?attrs.graphHeight:"300px"
            var graphAttrs = {
                                 domain: parseInt(attrs.domain?attrs.domain:10),
                                 range: parseInt(attrs.range?attrs.range:10),
                                 scale: parseFloat(attrs.scale?attrs.scale:1),
                                 domainLabel: attrs.domainLabel,
                                 rangeLabel: attrs.rangeLabel,
                                 tickLabelFrequency: attrs.tickLabelFrequency
                             }
            element.find('[jsx-graph]').css({width: width, height: height})
            element.find('[jsx-graph]').attr(graphAttrs)
            //element.find("#initialParams").remove()
            return function(scope, element, attrs, AssessmentItemController){
                scope.scale = graphAttrs.scale;
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController
                scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph")
                scope.controller.setResponse(scope.responseIdentifier,null)
                scope.outcomeReturned = scope.locked = attrs.hasOwnProperty('locked')?true:false
            }
        }
    }
})