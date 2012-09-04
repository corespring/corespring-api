#!/usr/bin/ruby

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


