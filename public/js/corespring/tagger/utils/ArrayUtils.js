(function () {

    window.com = (window.com || {}  );
    com.corespring = ( com.corespring || {});
    com.corespring.utils = (com.corespring.utils || {});

    var ArrayUtils = function () {

    };


    ArrayUtils.prototype.deconstruct = function (array, collection) {

        if (array.length == 0) {
            return;
        }

        function assemble(array, value) {
            if (value === undefined) {
                return array;
            }
            if (array[array.length - 1] + 1 == value) {
                array.push(value);
                return array;
            }
            else {
                return [value];
            }
        }

        var item = array.shift();

        if (collection.length == 0) {
            collection.push([item]);
        }
        var lastItem = collection[collection.length - 1];
        lastItem = assemble(lastItem, array[0]);

        if (lastItem !== collection[collection.length - 1]) {
            collection.push(lastItem);
        }

        this.deconstruct(array, collection);
    };

    ArrayUtils.prototype.prettyPrint = function (array) {
        var out = [];
        for (var i = 0; i < array.length; i++) {
            var arr = array[i];
            if (arr.length <= 2) {
                out.push(arr.join(","));
            }
            else {
                out.push(arr[0] + "-" + arr[arr.length - 1]);
            }
        }
        return out.join(",");
    };


    com.corespring.utils.ArrayUtils = new ArrayUtils();

}());
