#!/usr/bin/env ruby
require 'json'

puts "-------------------------------------"
puts "common/rollback/before/run_rollback.rb"
puts "rollback db migrations"
puts "-------------------------------------"

raise "no commit hash specified" if ARGV[0] == nil
raise "no config json file specified" if ARGV[1] == nil

puts "ARGV[0]: #{ARGV[0]}"
puts "ARGV[1]: #{ARGV[1]}"

config = JSON.parse(IO.read(ARGV[1]))
COMMIT_HASH = ARGV[0]
JAR="deployment/libs/mongo-migrator_2.9.2-0.1-SNAPSHOT-one-jar.jar"
MIGRATIONS="deployment/migrations"
MONGO_URI = config["HH_MONGO_URI"]

cmd = "java -jar #{JAR} rollback #{COMMIT_HASH} #{MONGO_URI} #{MIGRATIONS}"
puts "--------------------"
puts "command: "
puts "#{cmd}"
puts "--------------------"
`#{cmd}`
exit_code = $?.to_i
puts "||--------------> exit_code: #{exit_code}"
raise "An error occured running the script: #{cmd}" unless exit_code == 0