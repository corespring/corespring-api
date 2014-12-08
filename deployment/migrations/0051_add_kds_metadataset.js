var kdsMetadataKey = "kds";

function up() {
  db.metadataSets.insert({
    editorLabel: "Key Data Systems",
    isPublic: false,
    metadataKey: kdsMetadataKey,
    schema: [
      {
        "key" : "sourceId"
      },
      {
        "key" : "scoringType"
      }
    ]
  });
}

function down() {
  db.metadataSets.remove({"metadataKey": kdsMetadataKey}, true);
}
