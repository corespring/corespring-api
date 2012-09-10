/**
 * Lookup for all ajax services.
 */
//window.servicesModule
angular.module('tagger.services')
    .factory('ServiceLookup', function () {

        var ServiceLookup = function () {

            this.services = {
                //TODO: Do we need method here too? eg POST/PUT
                materials: '/api/v1/items/:itemId/materials',
                file: '/api/v1/items/:itemId/:resourceType/:resourceName',
                createSupportingMaterial: '/api/v1/items/{itemId}/materials',
                getAccessToken:'/web/access_token',
                items:'/api/v1/items/:id',
                uploadFileToMaterialResource: '/api/v1/items/{itemId}/materials/{materialName}/{filename}/upload',
                //items: '/assets/mock-json/:id',
                previewFile:'/web/runner/{key}',
                uploadSupportingMaterial:'/api/v1/items/{itemId}/{resourceName}/{filename}/upload',
                uploadFileToData: '/api/v1/items/{itemId}/data/{filename}/upload',
                deleteSupportingMaterial:'/api/v1/items/{itemId}/materials/{fileName}',
                standardsTree:'/assets/web/standards_tree.json',
                standards:'/api/v1/field_values/cc-standard',
                subject:'/api/v1/field_values/subject',
                collection:'/api/v1/collections',
                uploadFile:'/tagger/upload/{itemId}/{fileName}',
                viewFile:'/tagger/files/{itemId}/{fileName}',
                deleteFile:'/tagger/delete/{itemId}/{fileName}'
            };
        };

        ServiceLookup.CREATE_SUPPORTING_MATERIAL = "createSupportingMaterial";

        ServiceLookup.prototype.getUrlFor = function (name) {
            if (this.services.hasOwnProperty(name)) {
                return this.services[name];
            }
            throw "Can't find service for name: " + name;
        };

        return new ServiceLookup();
    });


