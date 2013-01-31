# A Db stored in the config

puts "Db class"
class Db

  URI_NO_USER = /mongodb:\/\/(.*):(.*)\/(.*$)/
  URI_USER = /mongodb:\/\/(.*):(.*)@(.*):(.*)\/(.*$)/

  attr_accessor :host, :username, :password, :port, :name

  def initialize(name, host, port, username=nil, password=nil)
    @host = host
    @port = port
    @name = name
    @username = username
    @password = password
  end

  def self.from_uri(uri)

    user,pwd,host,port,db = nil

    if( uri.match(URI_USER))
      match, user, pwd, host, port, name = *uri.match(URI_USER)
    elsif(uri.match(URI_NO_USER))
      match, host, port, name = *uri.match(URI_NO_USER)
      user = ""
      pwd = ""
    end

    return nil if( host.nil? || port.nil? || name.nil? )
    name.gsub!("\"", "")
    Db.new(name,host,port,user,pwd)
  end

  def authentication_required?
    has?(self.username) && has?(self.password)
  end

  def has?(s)
    !s.nil? && !s.empty?
  end


  def to_s
    user_pass = ""
    unless(@username.empty? || @password.empty? )
      user_pass = "#{@username}:#{@password}@"
    end
    "mongodb://#{user_pass}#{@host}:#{@port}/#{@name}"
  end

  def to_s_simple
    "#{@name} on #{@host}:#{@port} - (#{@username}:#{@password})"
  end

  def <=>(other)
    self.to_s <=> other.to_s
  end

end