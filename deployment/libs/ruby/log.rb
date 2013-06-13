require 'time'
def log(msg)
  puts "[#{Time.now.strftime("%I:%M%p")}] #{msg}"
end