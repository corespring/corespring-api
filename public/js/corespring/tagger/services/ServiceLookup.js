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

                createSupportingMaterialFile: '/api/v1/items/{itemId}/materials',
                deleteSupportingMaterialFile:'/api/v1/items/{itemId}/materials/{resourceName}/{filename}',
                updateSupportingMaterialFile: '/api/v1/items/{itemId}/materials/{resourceName}/{filename}',
                uploadSupportingMaterialFile:'/api/v1/items/{itemId}/materials/{resourceName}/{filename}/upload',

                items:'/api/v1/items/:id',
                getAccessToken:'/web/access_token',

                previewFile:'/web/runner/{key}',

                createDataFile: '/api/v1/items/{itemId}/data',
                deleteDataFile: '/api/v1/items/{itemId}/data/{filename}',
                updateDataFile: '/api/v1/items/{itemId}/data/{filename}',
                uploadDataFile: '/api/v1/items/{itemId}/data/{filename}/upload',

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

        ServiceLookup.prototype.getUrlFor = function (name, substitutions) {
            if (this.services.hasOwnProperty(name)) {

                var template = this.services[name];

                if( substitutions ){
                    for( var x in substitutions){
                        template = template.replace("{" + x + "}", substitutions[x]);
                    }
                }

                return template;
            }
            throw "Can't find service for name: " + name;
        };

        return new ServiceLookup();
    });


