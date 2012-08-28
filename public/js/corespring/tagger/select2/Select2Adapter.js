window.com = (window.com || {}  );
com.corespring = ( com.corespring || {});
com.corespring.select2 = (com.corespring.select2 || {});

/**
 * Select2Adapter. The Select2 directive expects an object with certain properties
 * and certain methods available. This is a default set for the choosers.
 * @param ajaxUrl
 * @param apiKey
 * @param placeholder
 * @param createQueryCallback
 * @param searchCategories
 * @constructor
 */
com.corespring.select2.Select2Adapter = function (ajaxUrl, apiKey, placeholder, createQueryCallback, searchCategories) {
    this.watch = "id";
    this.initial = function (url, currentValue, multiple) {
        var id = this.id(currentValue);
        return url + "/" + id + "?apiKey=" + apiKey;
    };
    this.allowClear = true;
    this.minimumInputLength = 1;
    this.placeholder = placeholder;
    this.id = function (item) {
        if (item == null) {
            return null;
        }
        if (item._id != null) {
            return item._id.$oid;
        }
        else if (item.refId != null) {
            return item.refId;
        }
        else {
            return null;
        }
    };
    /**
     * Callback from the select2 directive.
     * It allows us to create the object instead of using the id.
     * In this case we look up the mongo object in the result array of the ajax request.
     * We then copy it and insert a 'refId' into it.
     * //TODO: Should we insert the collection name too?
     * @param value
     * @return {*}
     */
    this.createValue = function (value) {

        var createReferenceObject = function (item) {
            var copy = angular.copy(item);
            if (!copy._id || !copy._id.$oid) {
                throw "Can't create reference - no $oid is set";
            }
            var refId = copy._id.$oid;
            copy._id = null;
            delete copy._id;
            copy.refId = refId;
            return copy;
        };

        var id = value;

        for (var i = 0; i < this.ajax.dataResults.length; i++) {
            var item = this.ajax.dataResults[i];
            var itemId = this.id(item);
            if (itemId == id) {
                console.log("found item");
                return createReferenceObject(item);
            }
        }

        return null;
    };

    this.formatSelection = function (item) {
        throw "You must override formatSelection"
    };
    this.formatResult = function (item) {
        throw "You ust override formatResult"
    };
    this.ajax = {
        url:ajaxUrl,
        dataType:'json',
        quietMillis:300,
        data:function (term, page) {
            return {
                q:createQueryCallback(term, searchCategories),
                l:50,
                apiKey:apiKey
            };
        },
        results:function (data, page) {
            this.dataResults = data;
            return {results:data};
        }
    }
};
