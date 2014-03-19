function up() {
    db.quizzes.renameCollection('assessments');
}

function down() {
    db.assessments.renameCollection('quizzes');
}