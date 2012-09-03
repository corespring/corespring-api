//ResourceEditorSpec.js
'use strict';

describe('ResourceEditor should', function () {

  beforeEach( function(){
    window.servicesModule
           .factory('AccessToken', [ function () {
              return { token: "1" };
            }]
        );

  })
  var scope, ctrl, $httpBackend;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
      $httpBackend = _$httpBackend_;
      //prepareBackend($httpBackend);
      scope = $rootScope.$new();

      try {
          ctrl = $controller(ResourceEditor, {$scope:scope});
      } catch (e) {
          throw("Error with the controller: " + e);
      }
      console.log(ctrl);
  }));


  it('init correctly', inject(function($rootScope){
    expect(scope.selectedFileImageUrl).toEqual('/assets/images/empty.png');
    expect(scope.showEditor).toEqual(false);
  }));


  it('handle enterEditor', inject(function($rootScope){
    expect(scope.resourceToEdit).toBe(undefined);
    var resource = { name: "testResource", files: [
      { name: "testFile", contentType: "xml", content: "<root/>", default: true}
    ]};

    scope.$apply( function(){
      $rootScope.$broadcast('enterEditor', resource, false);
    })

    expect(scope.resourceToEdit).toNotBe(null);
    expect(scope.resourceToEdit.files.length).toEqual(1);
    expect(scope.selectedFile.name).toEqual("testFile");

  }));
});


