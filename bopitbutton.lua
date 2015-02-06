require "cord"
sh = require "stormsh"
Button = require "Button"
LED = require "Led"

ipaddr = storm.os.getipaddr()

port = 227
count = 0
-- create client socket
csock = storm.net.udpsocket(port,
                            function(payload, from, port)
				print("Got response: %s", payload)
                            end)

function sendCommand(axis)
	local msg = string.format("%s", axis)
	print("sent: ", msg, " count: ",  count)
	storm.net.sendto(csock, msg, "ff02::1", port)
	count = count +1 
end


function start()
	b1 = Button:new("D9")
	b2 = Button:new("D10")
	b3 = Button:new("D11")
	b1:whenever("FALLING", function()
		sendCommand("x")
	end)
	b2:whenever("FALLING", function()
		sendCommand("y")
	end)
	b3:whenever("FALLING", function()
		sendCommand("z")
	end)
end

sh.start()
cord.enter_loop()
start()
