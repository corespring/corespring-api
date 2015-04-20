angular.module('tagger.directives')
  .directive('itemActionButton', [function () {

    function link($scope, $element, $attrs, ngModel){}

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        item: '=',
        publish: '&',
        cloneItem: '&',
        deleteItem: '&'
      },
      template: [
      '<div ng-show="!item.readOnly">',
      '<a class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" href="#menu-item-{{item.id}}">actions <b class="caret"></b></a>',
      '<div class="dropdown" id="menu-item-{{item.id}}">',
        '<ul class="dropdown-menu dropdown-menu-actions">',
          '<li>',
          '  <button class="btn btn-sm" ng-click="cloneItem(item)"><i class="fa fa-plus-circle"></i>&nbsp;clone</button>',
          '</li>',
          '<li ng-show="!item.published">',
          '  <button class="btn btn-sm btn-info" ng-click="publish(item)"><i class="fa fa-bolt"></i>&nbsp;publish</button>',
          '</li>',
          '<li>',
          '  <button ng-click="deleteItem(item)" class="btn btn-danger btn-sm"><i class="fa fa-trash-o"></i>&nbsp;delete</button>',
          '</li>',
        '</ul>',
      '</div>',
      '</div>'].join('')
    };
  }]);
