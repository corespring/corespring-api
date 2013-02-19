class MongoToolsException < RuntimeError
  attr :cmd, :output
  def initialize(cmd, output)
    @cmd = cmd 
    @output =output 
  end
end


class MongoTools

  # wrapper for monogdump shell command
  def self.dump(host,port,db,output,username = "", password = "")

    options = []
    options << o("-h", "#{host}:#{port}")
    options << o("-db", db)
    options << o("-o", output)
    options << o("-u", username)
    options << o("-p", password)

    cmd = "mongodump "

    options.each do |o|
      cmd << "#{o.key} #{o.value} " unless o.empty?
    end
    puts "MongoTools::dump - run: #{cmd}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0
  end

  # wrapper for mongorestore shell command
  def self.restore(host,port,db,source_folder,username = "", password = "")

    options = []
    options << o("-h", "#{host}:#{port}")
    options << o("-db", db)
    options << o("-u", username)
    options << o("-p", password)

    cmd = "mongorestore "

    options.each do |o|
      cmd << "#{o.key} #{o.value} " unless o.empty?
    end
    #ensure that we drop everything before we restore.
    cmd << "--drop "
    cmd << "#{source_folder}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0
  end

  private 
  def self.o(key,value)
    Option.new(key,value)
  end

end

class Option
  attr_accessor :key, :value

  def initialize(key,value)
    @key = key
    @value = value
  end

  def empty?
    @value.nil? || @value.empty?
  end
end

