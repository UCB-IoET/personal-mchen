require "cord"
sh = require "stormsh"
Button = require "button"
LED = require "led"
shield = require "starter"

ipaddr = storm.os.getipaddr()

port = 227
count = 0
time_gap = 3000
-- create client socket
csock = storm.net.udpsocket(port,
	function(payload, from, port)
		print("Got response: %s", payload)
		end)

function sendCommand(axis)
	local new_time = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
	local old_time = last_command_time
	if new_time - old_time < time_gap then return false end

	last_command_time = new_time
	local msg = string.format("%s", axis)
	print("sent: ", msg, " count: ",  count)
	storm.net.sendto(csock, msg, "ff02::1", port)
	count = count + 1 
	cord.new(function() shield.Buzz.go(80) cord.await(storm.os.invokeLater, 500*storm.os.MILLISECOND) shield.Buzz.stop() end)
	return true
end

function start()
	last_command_time = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
	blue = LED:new("D2")
	green = LED:new("D3")
	red = LED:new("D4")
	b1 = Button:new("D9")
	b2 = Button:new("D10")
	b3 = Button:new("D11")
	b1:whenever("FALLING", function()
		if sendCommand("x") then
			blue:flash(30)
			blue:off()
			blue:off()
		end
	end)
	b2:whenever("FALLING", function()
		if sendCommand("y") then
			green:flash(30)
			green:off()
			green:off()
		end
	end)
	b3:whenever("FALLING", function()
		if sendCommand("z") then
			red:flash(30)
			red:off()
			red:off()
		end
	end)
	print("setup complete")
end

start()
sh.start()
cord.enter_loop()