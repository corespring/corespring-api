#!/usr/bin/env ruby

remote = ARGV[0]
jar = "deployment/libs/heroku-helper_2.9.2-0.1-one-jar.jar"

puts "--------------------------------------------------"
puts "deploy.rb - remote: #{remote}"
puts "--------------------------------------------------"


raise "you must specify - #{remote}" if remote == nil

`git remote add #{remote} git@heroku.com:#{remote}.git`

raise "error adding git remote" unless $?.to_i == 0

`java -jar #{jar} push #{remote} master`

raise "error running helper" unless $?.to_i == 0
