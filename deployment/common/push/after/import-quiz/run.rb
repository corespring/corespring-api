#!/usr/bin/env ruby

require 'json'

puts "-------------------------------------"
puts "common/push/after/import-quiz/run.rb"
puts "-------------------------------------"
puts ""

raise "no config file specified" if ARGV[0].nil?

config = JSON.parse(IO.read(ARGV[0]))

db_uri = config["ENV_MONGO_URI"]
db_set_name = config["ENV_MONGO_REPLICA_SET_NAME"]

uri = db_set_name.nil? ? db_uri : "#{db_set_name}|#{db_uri}"

raise "no target db specified" if uri.nil?

cmd = "./deployment/common/push/after/import-quiz/import.rb \"#{uri}\" deployment/common/push/after/import-quiz/dump"
puts "command: "
puts cmd

process = IO.popen(cmd) do |io|
  while line = io.gets
    # the heroku-helper adds this to reset the ansi command - strip it
    cleaned = line.chomp!.gsub("[0m", "")
    print "[quiz-import] #{cleaned}\n" unless cleaned.empty?
  end
  io.close
  raise "An error occured" if $?.to_i != 0
end

puts ""
puts "-------------------------------------"




