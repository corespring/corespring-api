require 'time'
def log(msg)
  puts "[#{Time.now.utc.iso8601}] #{msg}"
end