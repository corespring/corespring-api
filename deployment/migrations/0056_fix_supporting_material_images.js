// This migration wraps all img tag in supporting material htmls into a div
// so that they will be picked up by the wiggi image feature
function replaceImgTags(term, replaceWith) {
  var cnt = 0;
  db.content.find().forEach(function(o) {
    var sm = o.supportingMaterials;
    if (sm && sm.length > 0) {
      for (var i = 0; i < sm.length; i++) {
        var files = sm[i].files;
        for (var j = 0; j < files.length; j++) {
          var file = files[j];
          if (file.contentType && file.contentType.toLowerCase() == "text/html") {
            if (file.content && file.content.indexOf("<img") >= 0) {
              cnt++;
              var newContent = file.content.replace(term, replaceWith);
              file.content = newContent;
              db.content.save(o);
            }
          }
        }
      }
    }
  });
  print(cnt + " img tags have been processed");
}

function up() {
  replaceImgTags(/(<img[\w\W]+?>)/gim, '<div>$1</div>');
}

function down() {
  replaceImgTags(/<div>(<img[\w\W]+?>)<\/div>/gim, '$1');
}
