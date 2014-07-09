//ResourceEditorSpec.js
'use strict';

describe('ResourceEditor should', function () {

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('AccessToken', {token:"1"});
        });
    });

    var broadcastEnterEditor = function ($rootScope, resource) {
        scope.$apply(function () {
            var urls = {
                createFile:"{filename}",
                uploadFile:"{filename}",
                updateFile:"{filename}",
                deleteFile:"{filename"
            };
            $rootScope.$broadcast('enterEditor', resource, false, urls);
        });
    };


    var scope, ctrl, $httpBackend;

    beforeEach(module('tagger.services'));

    var prepareBackend = function ($backend) {

        var urls = [
            {method:'PUT', url:/.*/, response:{ ok:true }},
            {method:'POST', url:/.*/, data: {}, response:{ ok:true }},
            {method:'DELETE', url:/.*/, data: {}, response:{ ok:true }}
        ];

        for (var i = 0; i < urls.length; i++) {
            var definition = urls[i];
            $backend.when(definition.method, definition.url).respond(200, definition.response);
        }
    };


    beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
        $httpBackend = _$httpBackend_;
        prepareBackend($httpBackend);
        scope = $rootScope.$new();

        try {
            ctrl = $controller(ResourceEditor, {$scope:scope});
        } catch (e) {
            throw("Error with the controller: " + e);
        }
    }));


    it('init correctly', inject(function ($rootScope) {
        expect(scope.selectedFileImageUrl).toEqual('/assets/images/empty.png');
        expect(scope.showEditor).toEqual(false);
    }));


    it('handle enterEditor', inject(function ($rootScope) {
        expect(scope.resourceToEdit).toBe(undefined);
        var resource = { name:"testResource", files:[
            { name:"testFile", contentType:"xml", content:"<root/>", default:true}
        ]};

        broadcastEnterEditor($rootScope, resource);

        expect(scope.resourceToEdit).toNotBe(null);
        expect(scope.resourceToEdit.files.length).toEqual(1);
        expect(scope.selectedFile.name).toEqual("testFile");

    }));

    it('rename file', inject(function ($rootScope) {
        var resource = { name:"testResource", files:[
            { name:"testFile.xml", contentType:"xml", content:"<root/>", default:true}
        ]};

        broadcastEnterEditor($rootScope, resource);

        scope.renameFile(scope.selectedFile);
        expect(scope.showRenameFileModal).toEqual(true);
        var newName = "myNewFileName.xml";
        scope.newFilename = newName;
        scope.confirmRenameFile();
        expect(scope.selectedFile.name).toEqual(newName);

    }));


    it('make default', inject(function ($rootScope) {
        var resource = { name:"testResource", files:[
            { name:"testFile.xml", contentType:"xml", content:"<root/>", default:true},
            { name:"file2.xml", contentType:"xml", content:"<root/>", default:false}
        ]};

        broadcastEnterEditor($rootScope, resource);

        expect(scope.selectedFile.name).toEqual("testFile.xml");
        scope.makeDefault(resource.files[1]);
        expect(resource.files[0].default).toEqual(false);
        expect(resource.files[1].default).toEqual(true);
    }));


    it('create new virtual file', inject(function ($rootScope) {
        var resource = { name:"testResource", files:[
            { name:"testFile.xml", contentType:"xml", content:"<root/>", default:true},
            { name:"file2.xml", contentType:"xml", content:"<root/>", default:false}
        ]};

        broadcastEnterEditor($rootScope, resource);

        scope.createNewVirtualFile("*.xml");
        $httpBackend.flush();
        expect(resource.files.length).toEqual(3);
        expect(scope.selectedFile).toBe(resource.files[2]);
    }));

    it('removing file needs confirmation', inject(function ($rootScope) {
        var resource = { name:"testResource", files:[
            { name:"testFile.xml", contentType:"xml", content:"<root/>", default:true}
        ]};
        broadcastEnterEditor($rootScope, resource);
        scope.removeFile(scope.selectedFile);
        expect(scope.showRemoveFileModal).toEqual(true);
        scope.confirmRemoveFile();
        $httpBackend.flush();
        expect(scope.resource.files.length).toEqual(0);
    }));

    /**
     * Fix for: CA-1665.
     * It's very hard to reproduce on the client, but on occasion the content property is set
     * for non text files - which causes the server to save the files as VirtualFiles not StoredFiles.
     */
    it('removes content prop from non text based files', inject(function($rootScope){
        var resource = { name:"testResource", files:[
            { name:"testFile.jpg", contentType:"image/jpg", content:"", default:true}
        ]};
        broadcastEnterEditor($rootScope, resource);
        scope.update(scope.selectedFile);
        $httpBackend.flush();
        expect(scope.selectedFile.content).toBe(undefined);

    }));

});


