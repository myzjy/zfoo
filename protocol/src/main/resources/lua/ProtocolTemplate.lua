---@class {}
local {} = class("{}")
function {}:ctor()
    {}
end

---@return {}
function {}:new({})
    {}
    return self
end

---@return number
function {}:protocolId()
    return {}
end

---@return string
function {}:write()
    local message = {
        protocolId = self:protocolId(),
        packet = {
            {}
        }
    }
    local jsonStr = JSON.encode(message)
    return jsonStr
end

---@return {}
function {}:read(data)
    {}
    local packet = self:new(
            {})
    return packet
end

{}

return {}
