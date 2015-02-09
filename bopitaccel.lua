require "cord"
Button = require "Button"
LED = require "Led"
math = require "math"

ipaddr = storm.os.getipaddr()

ACC = require "accel"
cord.new(function() 
	acc = ACC:new()
	acc:init()
	server()
end)


port = 227
dodgeTime = 2000
THRESHOLD = 1000
score = 0
-- create echo server as handler
server = function()
   ssock = storm.net.udpsocket(port,
                               function(payload, from, port)
				  if(payload) then
					cord.new(function()
						print(string.format("Dodge in the %s direction!", payload))
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
	local x, y, z = acc:get()
	local delta = {}
	delta["x"] = x - startACC["x"]
	delta["y"] = y - startACC["y"]
	delta["z"] = z - startACC["z"]
	print(string.format("deltas: %d %d %d\n", delta["x"], delta["y"], delta["z"]))	
	if(math.abs(delta[direction]) > THRESHOLD) then
		score = score + 1
		print("Successful Dodge! Score is now: "..score)
		return
	end
	
	local curTime = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
	--you didn't dodge, so check for timeout	
	if(curTime - startTime > dodgeTime) then
		score = score - 1
		print("You Failed to Dodge! Score is now "..score)		
		return
	else
		cord.await(storm.os.invokeLater,200*storm.os.MILLISECOND);
		dodgeCheckLoop(startACC, startTime, direction);
	end
end

cord.enter_loop()
