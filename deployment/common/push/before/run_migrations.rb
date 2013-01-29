#!/usr/bin/env ruby
require 'json'

puts "-------------------------------------"
puts "common/push/before/run.rb"
puts "running db migrations"
puts "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

JAR="deployment/libs/mongo-migrator_2.9.2-0.1-SNAPSHOT-one-jar.jar"
MIGRATIONS="deployment/migrations"
MONGO_URI = config["ENV_MONGO_URI"]
raise "Missing mongo uri" if MONGO_URI == nil

COMMIT_HASH = `git rev-parse --short HEAD`.chomp


cmd = "java -jar #{JAR} migrate #{COMMIT_HASH} #{MONGO_URI} #{MIGRATIONS}"
puts "command: "
puts "#{cmd}"
`#{cmd}`
exit_code = $?.to_i
puts "--------------> exit_code: #{exit_code}"
raise "An error occured running the script: #{cmd}" unless exit_code == 0