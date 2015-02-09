require "cord"
Button = require "Button"
LED = require "Led"
math = require "math"
LCD = require "lcd"

ipaddr = storm.os.getipaddr()

ACC = require "accel"
cord.new(function() 
	acc = ACC:new()
	acc:init()
	server()
	end)

lcd = LCD:new(storm.i2c.EXT, 0x7c, storm.i2c.EXT, 0xc4)
port = 227
dodgeTime = 500
THRESHOLD = 1000
score = 0
-- create echo server as handler
server = function()
ssock = storm.net.udpsocket(port,
	function(payload, from, port)
		if(payload) then
			cord.new(function()
				local s = string.format("Dodge %s direction!", payload)
				print(s)
				lcd:writeString(s)
				local startTime = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
				local x, y, z = acc:get()
				local startACC = {x=x, y=y, z=z}
				dodgeCheckLoop(startACC,startTime,payload)
				end)
		end
		end)
end

dodgeCheckLoop = function(startACC, startTime, direction)
	--get accelerometer data and check for dodge
	while true do 
		local x, y, z = acc:get()
		local delta = {}
		delta["x"] = x - startACC["x"]
		delta["y"] = y - startACC["y"]
		delta["z"] = z - startACC["z"]
		print(string.format("deltas: %d %d %d\n", delta["x"], delta["y"], delta["z"]))	
		if(math.abs(delta[direction]) > THRESHOLD) then
			score = score + 1
			local s = "Dodged! Score: "..score
			print(s)
			lcd:writeString(s)
			return
		end

		local curTime = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
		--you didn't dodge, so check for timeout	
		if(curTime - startTime > dodgeTime) then
			score = score - 1
			local s = "Hit! Score: "..score
			print(s)
			lcd:writeString(s)	
			return
		else
			cord.await(storm.os.invokeLater,200*storm.os.MILLISECOND);
		end
	end
end

cord.enter_loop()
