ACC = require "acc"
LED = require"led"
lcd = require "lcd"
require "cord"


--ip addr info to start
ipaddr = storm.os.getipaddr()
ipaddrs = string.format("%02x%02x:%02x%02x:%02x%02x:%02x%02x::%02x%02x:%02x%02x:%02x%02x:%02x%02x",
            ipaddr[0],
            ipaddr[1],ipaddr[2],ipaddr[3],ipaddr[4],
            ipaddr[5],ipaddr[6],ipaddr[7],ipaddr[8],    
            ipaddr[9],ipaddr[10],ipaddr[11],ipaddr[12],
            ipaddr[13],ipaddr[14],ipaddr[15])

print("ip addr", ipaddrs)
print("node id", storm.os.nodeid())
-- end ip addr info

--display init
disp = lcd:new(storm.i2c.EXT, 0x7c, storm.i2c.EXT, 0xc4)

brd = LED:new("GP0")
count = 0
cport = 49152
udpport = 227

csock = storm.net.udpsocket(cport, 
                function(payload, from, port)
                   brd:flash(3)
                   print (string.format("echo from %s port %d: %s",from,port,payload))
                end)

function server()
  acc = ACC:new()
  acc:init()
  ssock = storm.net.udpsocket(udpport, 
    function(payload, from, port)
      local s = string.format("from %s port %d: %s",from,port,payload)
      print (s)
      cord.new(function() 
        disp:writeString(s)
        local x,y,z = acc:get() 
        cord.await(storm.os.invokeLater, storm.os.SECOND)
        end)     
      end)
end

function client(msg)
   brd:flash(20)
   local msg = 
   print("send:", msg)
   -- send upd echo to link local all nodes multicast
   storm.net.sendto(csock, msg, "ff02::1", udpport) 
   count = count + 1
end

function loop() 
    acc = ACC:new()
    acc:init()
    print("start loop")
    while true do 
        local x = acc:get()
        local s = string.format("0x%04x says level=%d, count=%d", storm.os.nodeid(), level, count)
        client(x)        
        cord.await(storm.os.invokeLater, storm.os.SECOND) 
    end 
end

-- cord.new(loop)
cord.new(function() disp:clear() server() end)
cord.enter_loop()