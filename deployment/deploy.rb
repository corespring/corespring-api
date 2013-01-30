#!/usr/bin/env ruby

remote = ARGV[0]

puts "--------------------------------------------------"
puts "deploy.rb - remote: #{remote}"
puts "--------------------------------------------------"


raise "you must specify - #{remote}" if remote == nil

`git init`

`git remote add #{remote} git@heroku.com:#{remote}.git`

raise "error adding git remote" unless $?.to_i == 0

`java -jar deployment/libs/heroku-helper.jar push #{remote} master`

raise "error running helper" unless $?.to_i == 0
