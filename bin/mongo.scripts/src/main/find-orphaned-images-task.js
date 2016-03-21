
function FindOrphanedImagesTask() {

  this.findString = findString;
  this.isImage = isImage;
  this.isReferenced = isReferenced;
  this.findOrphanedImages = findOrphanedImages;
  this.run = run;

  //--------------------------

  function findString(o, s) {
    var es = encodeURI(s);
    var ds = decodeURI(s);

    function rekursiveFindString(o) {
      if (o === null || o === undefined) {
        return false;
      }
      var tpo = typeof o;
      if (tpo === 'string') {
        return o.length && (o.indexOf(s) >= 0 || o.indexOf(es) >= 0 || o.indexOf(ds) >= 0);
      }
      if (tpo === 'object') {
        for (var prop in o) {
          if (rekursiveFindString(o[prop])) {
            return true;
          }
        }
      }
      return false;
    }

    return rekursiveFindString(o);
  }

  function isImage(file) {
    return file && file.contentType && file.contentType.match(/image/);
  }

  function isReferenced(playerDefinition, file) {
    for (var s in playerDefinition) {
      if (s !== 'files') {
        if (findString(playerDefinition[s], file.name)) {
          return true;
        }
      }
    }
    return false;
  }

  function findOrphanedImages(item) {
    var orphaned = [];
    var pd = item.playerDefinition;
    for (var i = 0; i < pd.files.length; i++) {
      var file = pd.files[i];
      if (isImage(file) && !isReferenced(pd, file)) {

        orphaned.push(file);
      }
    }
    return orphaned;
  }

  function run(db){
    db.content.find({"playerDefinition.files.contentType": /image/}).forEach(function(item) {
      var orphaned = findOrphanedImages(item);
      if (orphaned.length > 0) {
        print({
          item: item,
          orphanedFiles: orphaned
        });
      }
    });
  }
}

//var task = new FindOrphanedImagesTask();
//task.run(db);