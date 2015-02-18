require "cord"
sh = require "stormsh"
Button = require "button"
LCD = require "lcd"

local set = "set"

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
local remote_map = {} -- [counter]: from

local port = 1525
local sock = storm.net.udpsocket(port,
	function(payload, from, port)
		local response = storm.mp.unpack(payload)
		print(string.format("Services from %s", from))
		printServicePairs(response)
		for k,v in response do
			if response[k][s] == "setBool" or response[k][s] == "setLed" then
				response[k][set] = false
				response[k]["func_name"] = k
				if remote_services[from] == nil then -- note: only supports one setBool per remote
					remote_map[num_remote] = from
					num_remote = num_remote + 1
				end
				remote_services[from] = response[k]
			end
		end
	end)

function invoke_bool(idx)
	local service = remote_services[remote_map[idx]]
	local new_bool = not service[set]
	service[set] = new_bool
	local msg = storm.mp.pack({service["func_name"], {new_bool}})
	storm.net.sendto(sock, msg, remote_map[idx], 1526)
end

function disp_lcd(idx)
	lcd:clear()
	lcd:setCursor(0, 0)
	lcd:writeString(remote_map[idx])
	lcd:setCursor(1, 0)
	lcd:writeString(remote_services[remote_map[idx]]["func_name"])
end


function svc_stdout(from_ip, from_port, msg)
  print (string.format("[STDOUT] (ip=%s, port=%d) %s", from_ip, from_port, msg))
end

function start()
	b1 = Button:new("D9")
	b2 = Button:new("D10")
	b3 = Button:new("D11")
	curr = 0
	num_remote = 0

	b1:whenever("FALLING", function()
		invoke_bool(curr)
	end)

	b2:whenever("FALLING", function()
		curr = curr + 1
		if curr > num_remote then curr = 0 end
		disp_lcd(curr)
	end)

	b3:whenever("FALLING", function()
		curr = curr - 1
		if curr < 0 then curr = num_remote end
		disp_lcd(curr)
	end)
end

lcd = LCD:new(storm.i2c.EXT, 0x7c, storm.i2c.EXT, 0xc4)

cord.new(function() lcd:init(2,1) end)
start()
sh.start()
cord.enter_loop()