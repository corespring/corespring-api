function hasKey(it, key) {
    for (var i = 0; i<it.length; i++) {
        if (it[i].key == key) return true;
    }
    return false;
}

db["fieldValues"].find().forEach( function(s) {
    var itemTypes = s.itemTypes;
    if (!hasKey(itemTypes, "FT"))
        itemTypes.push({key: 'FT', value: 'Focus Task'});
    else
        print("Focus Task is already present");

    if (!hasKey(itemTypes, "OR"))
        itemTypes.push({key: 'OR', value: 'Ordering'});
    else
        print("Ordering task is already present");

    db["fieldValues"].save(s);
})
