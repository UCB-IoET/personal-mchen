require "cord"
sh = require "stormsh"
Button = require "button"
LCD = require "lcd"

local curr_count = 0
local num_remote = 0

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
local remote_map = {}

local port = 1525
local sock = storm.net.udpsocket(port,
	function(payload, from, port)
		local response = storm.mp.unpack(payload)
		print(string.format("Services from %s", from))
		printServicePairs(response)
		local team_name = nil

		for k,v in pairs(response) do
			if remote_services[from] == nil and (response[k]["s"] == "setBoolean" or response["s"] == "setLed") then
				response[k][set] = false
				response[k]["func_name"] = k
				remote_services[from] = response[k]
				remote_map[curr_count] = from
				curr_count = curr_count + 1
				print("added "..from)
			end

			if k == "id" then team_name = v end
		end

		if remote_services[from] ~= nil then remote_services[from]["team_name"] = team_name end
	end)


function disp_lcd(idx)
	local v1 = remote_map[idx]
	if remote_services[remote_map[idx]]["team_name"] ~= nil then v1 = remote_services[remote_map[idx]]["team_name"] end
	local v2 = remote_services[remote_map[idx]]["func_name"]
	write_to_lcd(v1, v2)
end

function lcd_setup()
    lcd = LCD.new(storm.i2c.EXT, 0x7c, storm.i2c.EXT, 0xc4)
end

function write_to_lcd(value1, value2)
    cord.new(function ()
        lcd.init(2, 1)
        lcd.writeString(value1)
        lcd.setBacklight(150, 50, 220)
        if value2 ~= nil then
        	lcd.setCursor(1, 0)
        	lcd.writeString(value2)
        end
    end)
end

function invoke_bool(idx)
	local service = remote_services[remote_map[idx]]
	local new_bool = not service[set]
	service[set] = new_bool
	local msg = storm.mp.pack({service["func_name"], {new_bool}})
	storm.net.sendto(sock, msg, remote_map[idx], 1526)
	print("sent command to "..remote_map[idx])
end

function start()
	b1 = Button:new("D9")
	b2 = Button:new("D10")
	b3 = Button:new("D11")

	curr_count = 0

	b3:whenever("FALLING", function()
		invoke_bool(curr_count)
	end)

	b2:whenever("FALLING", function()		
		curr_count = curr_count + 1
		if curr_count > num_remote then curr_count = 0 end
		disp_lcd(curr_count)
	end)

	b1:whenever("FALLING", function()
		curr_count = curr_count - 1
		if curr_count < 0 then curr_count = num_remote end
		disp_lcd(curr_count)
	end)
end


lcd_setup()
start()
--sh.start()
cord.enter_loop()