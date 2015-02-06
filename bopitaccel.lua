require "cord"
sh = require "stormsh"
Button = require "Button"
LED = require "Led"

ipaddr = storm.os.getipaddr()

ACC = require"accel"
cord.new(function() 
		acc = ACC:new()
		acc:init()
end)

port = 227
count = 0
dodged = 0
dodgeTime = 1000
-- create echo server as handler
server = function()
   ssock = storm.net.udpsocket(port,
                               function(payload, from, port)
                                  print (string.format("from %s port %d: %s",from,port,payload))
				  if(payload) then
					dodged = 0
					local startTime = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
					local startACC = {}
					x, y, z = acc:get()
					startACC["x"] = x
					startACC["y"] = y
					startACC["z"] = z
					cord.new(function() dodgeCheckLoop(startACC, startTime, payload) end)	--start loop to check acc

				  end
                               end)
end

server()                        -- every node runs the echo server


dodgeCheckLoop = function(startACC, startTime, direction)
	curTime = storm.os.now(storm.os.SHIFT_0)/storm.os.MILLISECOND
	--get accelerometer data and check for dodge
	x, y, z = acc:get()

	deltaX = x - startACC["x"]
	deltaY = y - startACC["y"]
	deltaZ = z - startACC["z"]
	print(string.format("deltas: %d %d %d\n", deltaX, deltaY, deltaZ))
	--you didn't dodge, so check for timeout	
	if(curTime - startTime > dodgeTime) then
		print("You Failed to Dodge")
	else
		cord.await(storm.os.invokeLater,10*storm.os.MILLISECOND);
		dodgeCheckLoop(startTime, direction);
	end
end

checkDodged = function()
	if(dodged > 0) then
		print("You dodged it! Good job.")
	else
		print("You lose horribly")
	end
end

sh.start()
cord.enter_loop()
