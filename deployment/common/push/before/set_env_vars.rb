#!/usr/bin/env ruby

puts "-------- set env vars using heroku helper env conf file -------------"
### Args: 0: tmp json file with heroku config, 1: app name, 2: branch name
app = ARGV[1].chomp
puts "app name: #{app}"
cmd = `java -jar deployment/libs/heroku-helper_2.9.2-0.1-one-jar.jar set-env-vars #{app}`
puts "command: "
puts "#{cmd}"
`#{cmd}`
exit_code = $?.to_i
puts "--------------> exit_code: #{exit_code}"
raise "An error occured running the script: #{cmd}" unless exit_code == 0
