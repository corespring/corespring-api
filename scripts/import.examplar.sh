#!/usr/bin/ruby
puts "hello"
puts "Step 0: empty the db"
puts `mongo tdb --eval "db.dropDatabase()"`
puts "Step 1: port the content tagger db to a corespring-api format"
Dir.chdir('mongo.scripts/port_contentTagger_to_corespring-api')
puts "now in: #{Dir.pwd}"
puts `ruby run.rb`
Dir.chdir('../../')
puts "now in: #{Dir.pwd}"
puts "Step 2: run the corespring-content import script - please wait this can take some time"
puts `ruby update_mongo_document_from_corespring_content.rb`