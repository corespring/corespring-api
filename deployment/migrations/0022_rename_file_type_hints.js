//repackaging has taken place - update type hints
//models.item.resource => org.corespring.platform.core.models.item.resource

function up() {
    var processor = function (f) {
        var changed = false;
        if (f._t && f._t.indexOf("models") == 0) {
            f._t = "org.corespring.platform.core." + f._t;
            changed = true;
        }
        return changed;
    }
    run(processor);
}

function down(){
    var processor = function(f){
        if(f._t && f._t.indexOf("org.corespring.platform.core.") == 0){
            f._t = f._t.replace("org.corespring.platform.core.", "");
            return true;
        }
        return false;
    }
    run(processor);
}

function run(processor) {

    function runProcessorAgainstFiles(filesArray, processor) {

        if (!filesArray) {
            return false;
        }
        var changed = false;
        for (var i = 0; i < filesArray.length; i++) {
            var f = filesArray[i];
            var fileChanged = processor(f);
            if(fileChanged){
                changed = true;
            }
        }
        return changed;
    }

    var dataFile = {"data.files._t": {$exists: true}};
    var suFile = {"supportingMaterials.files._t": {$exists: true}};

    var together = { $or: [ dataFile, suFile ]};

    db.content.find(together).forEach(function (i) {

        var changed = false;

        var fileChange = runProcessorAgainstFiles(i.data.files, processor);

        if(fileChange){
            changed = true;
        }

        if (i.supportingMaterials) {
            for (var x = 0; x < i.supportingMaterials.length; x++) {
                var sm = i.supportingMaterials[x];
                var smChanged = runProcessorAgainstFiles(sm.files, processor);

                if (smChanged) {
                    changed = true;
                }
            }
        }

        if (changed) {
            db.content.save(i);
        }
    });
}


