var renameMap = [
    {
        "key":"Knowledge (Remember)",
        "value":"Remembering"
    },
    {
        "key":"Understand (Describe, dramatize)",
        "value":"Understanding"
    },
    {
        "key":"Apply",
        "value":"Applying"
    },
    {
        "key":"Analyze",
        "value":"Analyzing"
    },
    {
        "key":"Evaluate",
        "value":"Evaluating"
    },
    {
        "key":"Create",
        "value":"Creating"
    }
];


function findMapped(val) {
    for (var i = 0; i < renameMap.length; i++)
        if (renameMap[i].key == val) return renameMap[i].value;

    return val;
}

db["fieldValues"].find().forEach( function(s) {

    for (var i=0; i< s.bloomsTaxonomy.length; i++) {
        var rec  = s.bloomsTaxonomy[i].key;
        s.bloomsTaxonomy[i].key = findMapped(rec);
        s.bloomsTaxonomy[i].value = findMapped(rec);
    }
    db["fieldValues"].save(s);

})

db["content"].find().forEach( function(s) {
    if (s.bloomsTaxonomy != undefined) {
        s.bloomsTaxonomy = findMapped(s.bloomsTaxonomy);
        db["content"].save(s);
    }

});

