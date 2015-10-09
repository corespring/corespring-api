angular.module('tagger.directives')
  .directive('itemActionButton', ['$log', function($log) {

    $log.log('itemActionButton loaded into angular');

    return {
      restrict: 'AE',
      replace: true,
      link: link,
      template: template(),
      scope: {
        item: '=',
        publish: '&',
        cloneItem: '&',
        deleteItem: '&'
      }
    };

    function link(scope, elem, attr) {
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
        '      <li>',
        '        <button class="btn btn-sm" ng-click="cloneItem(item)">',
        '          <i class="fa fa-copy"></i>&nbsp;clone',
        '        </button>',
        '      </li>',
        '      <li ng-show="!item.published">',
        '        <button class="btn btn-sm btn-info" ng-click="publish(item)">',
        '          <i class="fa fa-bolt"></i>&nbsp;publish',
        '        </button>',
        '      </li>',
        '      <li>',
        '        <button class="btn btn-danger btn-sm" ng-click="deleteItem(item)">',
        '          <i class="fa fa-trash-o"></i>&nbsp;delete',
        '        </button>',
        '      </li>',
        '    </ul>',
        '  </div>',
        '</div>'
      ].join('');
    }
  }]);
