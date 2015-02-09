ACC = require "acc"
LED = require"led"
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

brd = LED:new("GP0")
count = 0
cport = 49152
udpport = 227

csock = storm.net.udpsocket(cport, 
                function(payload, from, port)
                   brd:flash(3)
                   print (string.format("echo from %s port %d: %s",from,port,payload))
                end)

function client(level)
   brd:flash(20)
   local msg = string.format("0x%04x says level=%d, count=%d", storm.os.nodeid(), level, count)
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
        client(x)        
        cord.await(storm.os.invokeLater, storm.os.SECOND) 
    end 
end

cord.new(loop)

cord.enter_loop()