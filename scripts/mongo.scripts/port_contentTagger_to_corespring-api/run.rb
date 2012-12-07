#!/usr/bin/ruby

# A set of mongo scripts to port data from the content-tagger db -> the corespring-api db.
# Usage: run.rb drop_db from_db_name to_db_name
# drop_db: true|false
# from_db_name: the db from which to get the data (typically the content tagger db)
# to_db_name: the to copy to

puts ">>> Running port script"

from = ARGV[1] || "corespring-live"
to = ARGV[2] || "cs-api-test"
puts "from: #{from}"
puts "to: #{to}"
date = ARGV[3] || "2012.11.22"

if ARGV[0] == "true"
    puts "dropping the db"
    `mongo #{to} --eval 'db.dropDatabase();'`
end

scripts = [
"insertFieldValues.js",
"copySubjectAndStandards.js",
"portCollection.js",
"portTemplates.js",
"port_users.js"
 ]


#scripts.each do |s|
#    puts "!!! Running: #{s}"
#    puts `mongo #{s} --eval 'var from = "#{from}"; var to = "#{to}"'`
#end

ct_to_cs_api = "portContentTagger_to_CorespringApi.js"

year,month,day = date.split(".").map{ |v| Integer(v) }

# javascript dates are zero based - so drop it down one.
month = month - 1

date_string = "#{year}, #{month}, #{day}"

puts "date --> #{date_string}"

puts `mongo #{ct_to_cs_api} --eval 'var from = "#{from}"; var to = "#{to}"; var d = new Date(#{date_string});'`

