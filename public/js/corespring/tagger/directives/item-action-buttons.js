angular.module('tagger.directives')
  .directive('itemActionButton', ['$log', function ($log) {

    $log.log('itemActionButton loaded into angular');

    return {
      restrict: 'AE',
      replace: true,
      link: link,
      template: template(),
      controller: ['$scope', function($scope) {
        this.actionClicked = function(label) {
          $scope.actionClicked(label);
        };
      }],
      scope: {
        permission: '=',
        item: '=',
        publish: '&',
        clone: '&',
        'delete': '&',
        edit: '&'
      }
    };

    function link($scope, $elem, $attr){

        var actionsMap = {
          edit: $scope.edit,
          clone: $scope.clone,
          publish: $scope.publish,
          'delete': $scope['delete']
        };

        $scope.actionClicked = function(label){
            actionsMap[label]($scope.item);
        };
    }

    function template() {
      return [
        '<div ng-show="!item.readOnly">',
        '  <a class="btn btn-default btn-sm btn-action dropdown-toggle"',
        '     id="menu-item-{{item.id}}"',
        '     data-toggle="dropdown"',
        '     aria-haspopup="true"',
        '     aria-expanded="false"',
        '     href="#">',
        '    actions <b class="caret"></b>',
        '  </a>',
        '  <div class="dropdown" aria-labelledby="menu-item-{{item.id}}">',
        '    <ul class="dropdown-menu dropdown-menu-actions">',
        '      <li item-action-li icon="pencil" label="edit" enabled="permission.write"/>',
        '      <li item-action-li icon="copy" label="clone" enabled="permission.clone"/>',
        '      <li item-action-li icon="bolt" label="publish" enabled="permission.write && !item.published"/>',
        '      <li item-action-li button-class="btn-danger" icon="trash-o" label="delete" enabled="permission.write"/>',
        '    </ul>',
        '  </div>',
        '</div>'
      ].join('');
    }
  }])
    .directive('itemActionLi', [function(){

        var template = [
        '<li ng-show="enabled">',
        '  <button class="btn btn-sm {{buttonClass}}" ng-click="buttonClicked()">',
        '   <i class="fa fa-{{icon}}"></i>&nbsp;{{label}}',
        '  </button>',
        '</li>'
        ].join('');

        return {
            restrict: 'A',
            replace: true,
            template: template,
            link: function($scope, $elem, $attrs, controller){

                $scope.buttonClicked = function(){
                  controller.actionClicked($attrs.label);
                };
            },
            require: '^itemActionButton',
            scope: {
                buttonClass: '@',
                icon: '@',
                label: '@',
                item: '=',
                enabled: '=',
                action: '&'
            }
        };
    }]);
