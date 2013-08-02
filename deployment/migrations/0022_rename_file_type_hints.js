//repackaging has taken place - update type hints

function run(processor){

    function runProcessorAgainstFiles(filesArray, processor) {

        if (!filesArray) {
            return false;
        }
        var changed = false;
        for (var i = 0; i < filesArray.length; i++) {
            var f = filesArray[i];
            changed = changed ? true : processor(f);
        }
        return changed;
    }

    db.content.find({"data.files._t": {$exists: true}}).forEach(function (item) {
        var changed = runProcessorAgainstFiles(item.data.files, processor );
        if (changed) {
            db.content.save(item);
        }
    });

    db.content.find({"supportingMaterials.files._t": {$exists: true}}).forEach(function (item) {
        for( var i = 0 ; i < item.supportingMaterials.length; i++){
            var sm = item.supportingMaterials[i];
            var changed = runProcessorAgainstFiles(sm.files, processor);
            if(changed){
                db.content.save(item);
            }
        }
    });
}


function up() {
    var processor = function(f){
        if(f._t && f._t.indexOf("models") == 0){
            f._t = "org.corespring.platform.core." + f._t;
            return true;
        }
        return false;
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


