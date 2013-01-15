var num = 0;

function fixScoringGuide(o){
    for (var i=0; i< o.supportingMaterials.length; i++) {
        if (o.supportingMaterials[i].name == "ScoringGuide") {
            o.supportingMaterials[i].name = "Scoring Guide";
            num++;
        }
    }
    db.content.save(o);
}

db.content.find({supportingMaterials: {$elemMatch: {name: "ScoringGuide"}}}).forEach(fixScoringGuide);

print("Updated "+num+" records");