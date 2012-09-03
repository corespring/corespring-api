#!/usr/bin/ruby

puts ">>> Running port script"

if ARGV[0] == "true"
    puts "dropping the db"
    `mongo corespring-api-dev --eval 'db.dropDatabase();'`
end

puts `mongo portCollection.js`
puts `mongo portContentTagger_to_CorespringApi.js`

