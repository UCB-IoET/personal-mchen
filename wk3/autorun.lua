require "cord"
sh = require "stormsh"

function printServicePairs(t)
	for k,v in pairs(t) do
		if k == "id"  or k == "t" then
			print(k,v)	
		else
			print(k)
			printPairs(v, "  >> ")
		end
	end
end

function printPairs(t, indent)
	local indent = indent or ""
	for k,v in pairs(t) do
		print(indent..k,v)
	end
end

local remote_services = {} --[from]: table

local port = 1525
local sock = storm.net.udpsocket(port,
	function(payload, from, port)
		local response = storm.mp.unpack(payload)
		print(string.format("Services from %s", from))
		printServicePairs(response)
		if response["t"] == nil then response["t"] = storm.os.now(storm.os.SHIFT_16) end
		remote_services[from] = response
		end)

local svc_manifest = {id="MacNCheese", writeStdout={s="setString", desc="write stdout"}, getNow={s="getNumber", desc="time now"}}
local broadcast_handle = storm.os.invokePeriodically(storm.os.SECOND, function()
	svc_manifest["t"] = storm.os.now(storm.os.SHIFT_16)
	local msg = storm.mp.pack(svc_manifest)
	storm.net.sendto(sock, msg, "ff02::1", 1525)
	print("sent broadcast")
	end)

-- stdout listener
listener = storm.net.udpsocket(1526,
	function(payload, from, port)
		local response = storm.mp.unpack(payload)
		-- print("Got response (%s): %s", from, response)
		local func = response[1]
		if func == "setString" then
			svc_stdout(from, port, response)
		elseif func == 	"getNow" then
			getNow(port, from, response[2]["echotime"])
		end
		end)

function getNow(from_port, from_ip, echotime)
	local result = {time=storm.os.now(storm.os.SHIFT_16), echotime=echotime}
	local msg = storm.mp.pack(result)
	storm.net.sendto(listener, msg, from_ip, from_port)
end

function svc_stdout(from_ip, from_port, msg)
  print (string.format("[STDOUT] (ip=%s, port=%d) %s", from_ip, from_port, msg))
end

sh.start()
cord.enter_loop()