#!/usr/bin/ruby

# A set of mongo scripts to port data from the content-tagger db -> the corespring-api db.
# Usage: run.rb drop_db from_db_name to_db_name
# drop_db: true|false
# from_db_name: the db from which to get the data (typically the content tagger db)
# to_db_name: the to copy to

puts ">>> Running port script"

from = ARGV[1] || "corespring-live"
to = ARGV[2] || "tdb"
puts "from: #{from}"
puts "to: #{to}"

if ARGV[0] == "true"
    puts "dropping the db"
    `mongo #{to} --eval 'db.dropDatabase();'`
end

scripts = [
"insertFieldValues.js",
"copySubjectAndStandards.js",
"portCollection.js",
"port_users.js",
"portContentTagger_to_CorespringApi.js" ]

scripts.each do |s|
    puts "!!! Running: #{s}"
    puts `mongo #{s} --eval 'var from = "#{from}"; var to = "#{to}"'`
end


